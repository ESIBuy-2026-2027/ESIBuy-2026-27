package es.uclm.esibuy.auth.passwordreset.repository;

import java.time.Instant;
import java.util.Optional;

import es.uclm.esibuy.auth.passwordreset.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    void deleteAllByUserIdAndUsedAtIsNull(String userId);

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash,
            Instant now);
}
