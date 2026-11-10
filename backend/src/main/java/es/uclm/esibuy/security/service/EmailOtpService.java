
package es.uclm.esibuy.security.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import es.uclm.esibuy.common.exception.InvalidOtpException;
import es.uclm.esibuy.security.model.EmailOtpChallenge;
import es.uclm.esibuy.security.repository.EmailOtpChallengeRepository;
import es.uclm.esibuy.users.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_BOUND = 1_000_000;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(10);

    private static final String LOGIN = "LOGIN";
    private static final String DISABLE_EMAIL_OTP = "DISABLE_EMAIL_OTP";

    private final EmailOtpChallengeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpSender otpMailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailOtpService(
            EmailOtpChallengeRepository repository,
            PasswordEncoder passwordEncoder,
            EmailOtpSender otpMailSender) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.otpMailSender = otpMailSender;
    }

    public void issueChallenge(User user) {
        issueChallenge(user, LOGIN);
    }

    public void issueDisableChallenge(User user) {
        issueChallenge(user, DISABLE_EMAIL_OTP);
    }

    private void issueChallenge(User user, String purpose) {
        String code = String.format(
                Locale.ROOT,
                "%0" + OTP_LENGTH + "d",
                secureRandom.nextInt(OTP_BOUND));

        repository.deleteByUserIdAndPurpose(user.getId(), purpose);

        EmailOtpChallenge challenge = new EmailOtpChallenge(
                user.getId(),
                passwordEncoder.encode(code),
                Instant.now().plus(OTP_VALIDITY),
                purpose);

        repository.save(challenge);
        otpMailSender.sendOtp(user.getEmail(), code);
    }

    public void verifyChallenge(String userId, String code) {
        verifyChallenge(userId, code, LOGIN);
    }

    public void verifyDisableChallenge(String userId, String code) {
        verifyChallenge(userId, code, DISABLE_EMAIL_OTP);
    }

    private void verifyChallenge(
            String userId,
            String code,
            String purpose) {

        EmailOtpChallenge challenge = repository
                .findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                        userId, purpose)
                .orElseThrow(InvalidOtpException::new);

        if (!Instant.now().isBefore(challenge.getExpiresAt())
                || challenge.getAttempts() >= MAX_ATTEMPTS) {
            challenge.markUsed();
            repository.save(challenge);
            throw new InvalidOtpException();
        }

        if (code == null
                || !code.matches("\\d{6}")
                || !passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.incrementAttempts();

            if (challenge.getAttempts() >= MAX_ATTEMPTS) {
                challenge.markUsed();
            }

            repository.save(challenge);
            throw new InvalidOtpException();
        }

        challenge.markUsed();
        repository.save(challenge);
    }
}
