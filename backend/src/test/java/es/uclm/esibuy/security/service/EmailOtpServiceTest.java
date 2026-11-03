
package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import es.uclm.esibuy.security.model.EmailOtpChallenge;
import es.uclm.esibuy.security.repository.EmailOtpChallengeRepository;
import es.uclm.esibuy.users.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmailOtpServiceTest {

        @Mock
        private EmailOtpChallengeRepository repository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private EmailOtpSender otpMailSender;

        @Mock
        private User user;

        private EmailOtpService service;

        @BeforeEach
        void setUp() {
                service = new EmailOtpService(
                                repository,
                                passwordEncoder,
                                otpMailSender);
        }

        @Test
        void issuesLoginChallengeAndSendsOtp() {
                when(user.getId()).thenReturn("user-1");
                when(user.getEmail()).thenReturn("user@example.com");
                when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

                service.issueChallenge(user);

                ArgumentCaptor<EmailOtpChallenge> captor = ArgumentCaptor.forClass(EmailOtpChallenge.class);

                verify(repository).deleteByUserIdAndPurpose("user-1", "LOGIN");
                verify(repository).save(captor.capture());

                EmailOtpChallenge challenge = captor.getValue();

                assertEquals("user-1", challenge.getUserId());
                assertEquals("hashed-code", challenge.getCodeHash());
                assertEquals("LOGIN", challenge.getPurpose());
                assertEquals(0, challenge.getAttempts());
                assertFalse(challenge.isUsed());
                assertTrue(challenge.getExpiresAt().isAfter(Instant.now()));

                ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

                verify(otpMailSender).sendOtp(
                                org.mockito.ArgumentMatchers.eq("user@example.com"),
                                codeCaptor.capture());

                assertNotNull(codeCaptor.getValue());
                assertTrue(codeCaptor.getValue().matches("\\d{6}"));
        }

        @Test
        void issuesDisableEmailOtpChallengeWithSpecificPurpose() {
                when(user.getId()).thenReturn("user-1");
                when(user.getEmail()).thenReturn("user@example.com");
                when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

                service.issueDisableChallenge(user);

                ArgumentCaptor<EmailOtpChallenge> captor = ArgumentCaptor.forClass(EmailOtpChallenge.class);

                verify(repository).deleteByUserIdAndPurpose(
                                "user-1", "DISABLE_EMAIL_OTP");
                verify(repository).save(captor.capture());

                assertEquals(
                                "DISABLE_EMAIL_OTP",
                                captor.getValue().getPurpose());

                verify(otpMailSender).sendOtp(
                                org.mockito.ArgumentMatchers.eq("user@example.com"),
                                anyString());
        }

        @Test
        void verifiesValidLoginOtpAndMarksChallengeAsUsed() {
                EmailOtpChallenge challenge = validChallenge("LOGIN");

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.of(challenge));
                when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

                service.verifyChallenge("user-1", "123456");

                assertTrue(challenge.isUsed());
                verify(repository).save(challenge);
        }

        @Test
        void verifiesValidDisableOtp() {
                EmailOtpChallenge challenge = validChallenge("DISABLE_EMAIL_OTP");

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "DISABLE_EMAIL_OTP"))
                                .thenReturn(Optional.of(challenge));
                when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

                service.verifyDisableChallenge("user-1", "123456");

                assertTrue(challenge.isUsed());
                verify(repository).save(challenge);
        }

        @Test
        void rejectsVerificationWhenChallengeDoesNotExist() {
                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.empty());

                assertThrows(
                                RuntimeException.class,
                                () -> service.verifyChallenge("user-1", "123456"));

                verify(repository, never()).save(any(EmailOtpChallenge.class));
        }

        @Test
        void rejectsExpiredChallengeAndMarksItAsUsed() {
                EmailOtpChallenge challenge = new EmailOtpChallenge(
                                "user-1",
                                "hashed-code",
                                Instant.now().minusSeconds(1),
                                "LOGIN");

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.of(challenge));

                assertThrows(
                                RuntimeException.class,
                                () -> service.verifyChallenge("user-1", "123456"));

                assertTrue(challenge.isUsed());
                verify(repository).save(challenge);
                verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        void rejectsChallengeWithFivePreviousAttempts() {
                EmailOtpChallenge challenge = validChallenge("LOGIN");

                for (int i = 0; i < 5; i++) {
                        challenge.incrementAttempts();
                }

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.of(challenge));

                assertThrows(
                                RuntimeException.class,
                                () -> service.verifyChallenge("user-1", "123456"));

                assertTrue(challenge.isUsed());
                verify(repository).save(challenge);
                verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        void rejectsNullOtpAndIncrementsAttempts() {
                EmailOtpChallenge challenge = validChallenge("LOGIN");

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.of(challenge));

                assertThrows(
                                RuntimeException.class,
                                () -> service.verifyChallenge("user-1", null));

                assertEquals(1, challenge.getAttempts());
                assertFalse(challenge.isUsed());
                verify(repository).save(challenge);
                verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        void rejectsOtpWithInvalidFormatAndIncrementsAttempts() {
                EmailOtpChallenge challenge = validChallenge("LOGIN");

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.of(challenge));

                assertThrows(
                                RuntimeException.class,
                                () -> service.verifyChallenge("user-1", "12A456"));

                assertEquals(1, challenge.getAttempts());
                verify(passwordEncoder, never()).matches(anyString(), anyString());
                verify(repository).save(challenge);
        }

        @Test
        void rejectsIncorrectOtpAndIncrementsAttempts() {
                EmailOtpChallenge challenge = validChallenge("LOGIN");

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.of(challenge));
                when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(false);

                assertThrows(
                                RuntimeException.class,
                                () -> service.verifyChallenge("user-1", "123456"));

                assertEquals(1, challenge.getAttempts());
                assertFalse(challenge.isUsed());
                verify(repository).save(challenge);
        }

        @Test
        void marksChallengeAsUsedWhenFifthIncorrectAttemptIsReached() {
                EmailOtpChallenge challenge = validChallenge("LOGIN");

                for (int i = 0; i < 4; i++) {
                        challenge.incrementAttempts();
                }

                when(repository.findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                                "user-1", "LOGIN")).thenReturn(Optional.of(challenge));
                when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(false);

                assertThrows(
                                RuntimeException.class,
                                () -> service.verifyChallenge("user-1", "123456"));

                assertEquals(5, challenge.getAttempts());
                assertTrue(challenge.isUsed());
                verify(repository).save(challenge);
        }

        private EmailOtpChallenge validChallenge(String purpose) {
                return new EmailOtpChallenge(
                                "user-1",
                                "hashed-code",
                                Instant.now().plusSeconds(600),
                                purpose);
        }
}
