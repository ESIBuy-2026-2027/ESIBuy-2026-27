package es.uclm.esibuy.auth.passwordreset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import es.uclm.esibuy.auth.passwordreset.model.PasswordResetToken;
import es.uclm.esibuy.auth.passwordreset.repository.PasswordResetTokenRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

        private static final String USER_ID = "user-1";
        private static final String EMAIL = "usuario@example.com";
        private static final String RAW_TOKEN = "token-de-recuperacion";
        private static final String NEW_PASSWORD = "NuevaPassword123!";
        private static final String ENCODED_PASSWORD = "encoded-password";

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordResetTokenRepository tokenRepository;

        @Mock
        private PasswordResetRateLimiter rateLimiter;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private MongoTemplate mongoTemplate;

        @Mock
        private PasswordResetEmailService emailService;

        private PasswordResetService service;
        private User user;

        @BeforeEach
        void setUp() {
                service = new PasswordResetService(
                                userRepository,
                                tokenRepository,
                                rateLimiter,
                                passwordEncoder,
                                mongoTemplate,
                                emailService,
                                false,
                                "https://esibuy.example/");

                user = new User(EMAIL, "Ana", "García", UserRole.CLIENT);

                ReflectionTestUtils.setField(user, "id", USER_ID);
        }

        @Test
        void requestResetShouldNormalizeEmailAndCreateHashedToken()
                        throws Exception {

                when(rateLimiter.allow("127.0.0.1", EMAIL)).thenReturn(true);

                when(userRepository.findByEmailIgnoreCase(EMAIL))
                                .thenReturn(Optional.of(user));

                when(tokenRepository.save(any(PasswordResetToken.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                service.requestReset("  USUARIO@EXAMPLE.COM  ", "127.0.0.1");

                ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

                verify(tokenRepository).deleteAllByUserIdAndUsedAtIsNull(USER_ID);
                verify(tokenRepository).save(tokenCaptor.capture());

                ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

                verify(emailService).sendPasswordResetEmail(
                                eq(EMAIL),
                                urlCaptor.capture());

                PasswordResetToken savedToken = tokenCaptor.getValue();

                assertEquals(USER_ID, savedToken.getUserId());
                assertEquals(64, savedToken.getTokenHash().length());
                assertNotNull(savedToken.getCreatedAt());

                assertEquals(
                                15 * 60,
                                savedToken.getExpiresAt().getEpochSecond()
                                                - savedToken.getCreatedAt().getEpochSecond());

                String resetUrl = urlCaptor.getValue();

                assertTrue(resetUrl.startsWith(
                                "https://esibuy.example/restablecer-password?token="));

                String rawToken = resetUrl.substring(
                                resetUrl.indexOf("token=") + 6);

                assertEquals(64, rawToken.length());
                assertEquals(sha256(rawToken), savedToken.getTokenHash());
                assertTrue(!savedToken.getTokenHash().equals(rawToken));
        }

        @Test
        void requestResetShouldIgnoreRequestWhenRateLimitIsReached() {
                when(rateLimiter.allow("127.0.0.1", EMAIL)).thenReturn(false);

                service.requestReset(EMAIL, "127.0.0.1");

                verify(userRepository, never()).findByEmailIgnoreCase(any());
                verify(tokenRepository, never()).save(any());
                verify(emailService, never()).sendPasswordResetEmail(any(), any());
        }

        @Test
        void requestResetShouldNotRevealWhetherEmailExists() {
                when(rateLimiter.allow("127.0.0.1", EMAIL)).thenReturn(true);

                when(userRepository.findByEmailIgnoreCase(EMAIL))
                                .thenReturn(Optional.empty());

                service.requestReset(EMAIL, "127.0.0.1");

                verify(tokenRepository, never())
                                .deleteAllByUserIdAndUsedAtIsNull(any());

                verify(tokenRepository, never()).save(any());
                verify(emailService, never()).sendPasswordResetEmail(any(), any());
        }

        @Test
        void requestResetShouldContinueWhenEmailCannotBeSent() {
                when(rateLimiter.allow("127.0.0.1", EMAIL)).thenReturn(true);

                when(userRepository.findByEmailIgnoreCase(EMAIL))
                                .thenReturn(Optional.of(user));

                org.mockito.Mockito.doThrow(
                                new MailSendException("SMTP no disponible"))
                                .when(emailService)
                                .sendPasswordResetEmail(eq(EMAIL), any(String.class));

                service.requestReset(EMAIL, "127.0.0.1");

                verify(tokenRepository).save(any(PasswordResetToken.class));

                verify(emailService).sendPasswordResetEmail(
                                eq(EMAIL),
                                any(String.class));
        }

        @Test
        void requestResetShouldUseDevelopmentConfigurationWithoutChangingTokenStorage() {
                PasswordResetService developmentService = new PasswordResetService(
                                userRepository,
                                tokenRepository,
                                rateLimiter,
                                passwordEncoder,
                                mongoTemplate,
                                emailService,
                                true,
                                "http://localhost:4200///");

                when(rateLimiter.allow("127.0.0.1", EMAIL)).thenReturn(true);

                when(userRepository.findByEmailIgnoreCase(EMAIL))
                                .thenReturn(Optional.of(user));

                developmentService.requestReset(EMAIL, "127.0.0.1");

                ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

                verify(tokenRepository).save(tokenCaptor.capture());

                ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

                verify(emailService).sendPasswordResetEmail(
                                eq(EMAIL),
                                urlCaptor.capture());

                assertTrue(urlCaptor.getValue().startsWith(
                                "http://localhost:4200/restablecer-password?token="));

                assertEquals(64, tokenCaptor.getValue().getTokenHash().length());
        }

        @Test
        void validateResetTokenShouldAcceptTokenAfterTrimmingWhitespace() {
                when(tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                                eq(sha256(RAW_TOKEN)),
                                any(Instant.class)))
                                .thenReturn(Optional.of(token(RAW_TOKEN)));

                service.validateResetToken("  " + RAW_TOKEN + "  ");

                verify(tokenRepository).findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                                eq(sha256(RAW_TOKEN)),
                                any(Instant.class));
        }

        @Test
        void validateResetTokenShouldRejectNullOrBlankToken() {
                assertThrows(
                                PasswordResetService.InvalidPasswordResetTokenException.class,
                                () -> service.validateResetToken(null));

                assertThrows(
                                PasswordResetService.InvalidPasswordResetTokenException.class,
                                () -> service.validateResetToken("   "));

                verify(tokenRepository, never())
                                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(any(), any());
        }

        @Test
        void validateResetTokenShouldRejectTokenNotFoundOrExpired() {
                when(tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                                eq(sha256(RAW_TOKEN)),
                                any(Instant.class)))
                                .thenReturn(Optional.empty());

                assertThrows(
                                PasswordResetService.InvalidPasswordResetTokenException.class,
                                () -> service.validateResetToken(RAW_TOKEN));
        }

        @Test
        void confirmResetShouldUpdatePasswordAndInvalidateOtherTokens() {
                PasswordResetToken candidate = token(RAW_TOKEN);

                when(tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                                eq(sha256(RAW_TOKEN)),
                                any(Instant.class)))
                                .thenReturn(Optional.of(candidate));

                when(mongoTemplate.findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(PasswordResetToken.class)))
                                .thenReturn(candidate);

                when(userRepository.findById(USER_ID))
                                .thenReturn(Optional.of(user));

                when(passwordEncoder.encode(NEW_PASSWORD))
                                .thenReturn(ENCODED_PASSWORD);

                service.confirmReset(RAW_TOKEN, NEW_PASSWORD);

                assertEquals(ENCODED_PASSWORD, user.getPasswordHash());

                verify(userRepository).save(user);
                verify(tokenRepository).deleteAllByUserIdAndUsedAtIsNull(USER_ID);

                verify(mongoTemplate).findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(PasswordResetToken.class));
        }

        @Test
        void confirmResetShouldRejectTokenNotFoundOrExpired() {
                when(tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                                eq(sha256(RAW_TOKEN)),
                                any(Instant.class)))
                                .thenReturn(Optional.empty());

                assertThrows(
                                PasswordResetService.InvalidPasswordResetTokenException.class,
                                () -> service.confirmReset(RAW_TOKEN, NEW_PASSWORD));

                verify(mongoTemplate, never()).findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(PasswordResetToken.class));

                verify(userRepository, never()).save(any());
        }

        @Test
        void confirmResetShouldRejectTokenAlreadyConsumedAtomically() {
                when(tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                                eq(sha256(RAW_TOKEN)),
                                any(Instant.class)))
                                .thenReturn(Optional.of(token(RAW_TOKEN)));

                when(mongoTemplate.findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(PasswordResetToken.class)))
                                .thenReturn(null);

                assertThrows(
                                PasswordResetService.InvalidPasswordResetTokenException.class,
                                () -> service.confirmReset(RAW_TOKEN, NEW_PASSWORD));

                verify(userRepository, never()).findById(any());
                verify(userRepository, never()).save(any());
        }

        @Test
        void confirmResetShouldRejectWhenTokenOwnerNoLongerExists() {
                PasswordResetToken candidate = token(RAW_TOKEN);

                when(tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                                eq(sha256(RAW_TOKEN)),
                                any(Instant.class)))
                                .thenReturn(Optional.of(candidate));

                when(mongoTemplate.findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(PasswordResetToken.class)))
                                .thenReturn(candidate);

                when(userRepository.findById(USER_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                PasswordResetService.InvalidPasswordResetTokenException.class,
                                () -> service.confirmReset(RAW_TOKEN, NEW_PASSWORD));

                verify(passwordEncoder, never()).encode(any());
                verify(userRepository, never()).save(any());

                verify(tokenRepository, never())
                                .deleteAllByUserIdAndUsedAtIsNull(any());
        }

        private PasswordResetToken token(String rawToken) {
                Instant now = Instant.now();

                PasswordResetToken token = new PasswordResetToken(
                                sha256(rawToken),
                                USER_ID,
                                now,
                                now.plusSeconds(900));

                ReflectionTestUtils.setField(token, "id", "token-1");

                return token;
        }

        private String sha256(String value) {
                try {
                        return HexFormat.of().formatHex(
                                        MessageDigest.getInstance("SHA-256")
                                                        .digest(value.getBytes(StandardCharsets.UTF_8)));

                } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                }
        }
}