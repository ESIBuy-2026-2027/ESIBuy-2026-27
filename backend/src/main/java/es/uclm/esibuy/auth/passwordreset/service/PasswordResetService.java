package es.uclm.esibuy.auth.passwordreset.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import es.uclm.esibuy.auth.passwordreset.model.PasswordResetToken;
import es.uclm.esibuy.auth.passwordreset.repository.PasswordResetTokenRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetRateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    private final boolean logRawTokenInDevelopment;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetRateLimiter rateLimiter,
            PasswordEncoder passwordEncoder,
            MongoTemplate mongoTemplate,
            @Value("${esibuy.password-reset.log-token:false}") boolean logRawTokenInDevelopment) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
        this.logRawTokenInDevelopment = logRawTokenInDevelopment;
    }

    public void requestReset(String email, String ipAddress) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (!rateLimiter.allow(ipAddress, normalizedEmail)) {
            LOGGER.info("Password reset request ignored by rate limiter");
            return;
        }

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOptional.isEmpty()) {
            LOGGER.info("Password reset requested for an unregistered address");
            return;
        }

        User user = userOptional.get();

        // Invalidar cualquier token previo que siga sin utilizarse.
        tokenRepository.deleteAllByUserIdAndUsedAtIsNull(user.getId());

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = HexFormat.of().formatHex(randomBytes);
        Instant now = Instant.now();

        tokenRepository.save(new PasswordResetToken(
                sha256(rawToken),
                user.getId(),
                now,
                now.plus(TOKEN_LIFETIME)));

        /*
         * Solo para desarrollo local.
         * El token permite recuperar el acceso a la cuenta, por lo que
         * no debe registrarse en producción.
         */
        if (logRawTokenInDevelopment) {
            LOGGER.warn(
                    "[DEV ONLY] ESIBuy password reset token for user {}: {}",
                    user.getId(),
                    rawToken);
        }

        LOGGER.info(
                "Password reset token generated; expires in {} minutes",
                TOKEN_LIFETIME.toMinutes());

        // Cuando se integre el proveedor de correo, se enviará rawToken
        // al usuario. En la base de datos solo se almacena su hash.
    }

    public void validateResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }

        String tokenHash = sha256(rawToken.trim());
        Instant now = Instant.now();

        boolean valid = tokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                        tokenHash, now)
                .isPresent();

        if (!valid) {
            throw new InvalidPasswordResetTokenException();
        }
    }

    public void confirmReset(String rawToken, String newPassword) {
        String tokenHash = sha256(rawToken);
        Instant now = Instant.now();

        PasswordResetToken candidate = tokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                        tokenHash, now)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        /*
         * Consumo atómico: solo una petición concurrente puede marcar
         * el token como utilizado.
         */
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("id").is(candidate.getId()),
                Criteria.where("tokenHash").is(tokenHash),
                Criteria.where("usedAt").is(null),
                Criteria.where("expiresAt").gt(now)));

        Update update = new Update().set("usedAt", now);

        PasswordResetToken consumed = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                PasswordResetToken.class);

        if (consumed == null) {
            throw new InvalidPasswordResetTokenException();
        }

        User user = userRepository.findById(consumed.getUserId())
                .orElseThrow(InvalidPasswordResetTokenException::new);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidar cualquier otro token activo del usuario.
        tokenRepository.deleteAllByUserIdAndUsedAtIsNull(user.getId());

        LOGGER.info("Password reset completed for userId={}", user.getId());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available", exception);
        }
    }

    public static class InvalidPasswordResetTokenException
            extends RuntimeException {

        public InvalidPasswordResetTokenException() {
            super("El token no es válido, ha caducado o ya se ha utilizado.");
        }
    }
}
