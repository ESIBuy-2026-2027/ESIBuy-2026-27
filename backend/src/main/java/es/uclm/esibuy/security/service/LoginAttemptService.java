package es.uclm.esibuy.security.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import es.uclm.esibuy.security.model.LoginAttempt;
import es.uclm.esibuy.security.repository.LoginAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int BLOCK_THRESHOLD = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);
    private static final Duration FIRST_BLOCK = Duration.ofSeconds(30);
    private static final Duration SECOND_BLOCK = Duration.ofMinutes(2);
    private static final Duration THIRD_BLOCK = Duration.ofMinutes(4);
    private static final Duration MAX_BLOCK = Duration.ofMinutes(5);

    private final LoginAttemptRepository loginAttemptRepository;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository) {
        this(loginAttemptRepository, Clock.systemUTC());
    }

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository, Clock clock) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.clock = clock;
    }

    public boolean isBlocked(String email, String ipAddress) {
        return getRemainingBlockSeconds(email, ipAddress) > 0;
    }

    public long getRemainingBlockSeconds(String email, String ipAddress) {
        Instant blockedUntil = loginAttemptRepository
                .findByEmailAndIpAddress(normalizeEmail(email), ipAddress)
                .map(LoginAttempt::getBlockedUntil)
                .orElse(null);
        if (blockedUntil == null) {
            return 0;
        }

        long remainingMillis = Duration.between(Instant.now(clock), blockedUntil).toMillis();
        return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000;
    }

    public LoginAttempt loginFailed(String email, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        Instant now = Instant.now(clock);
        LoginAttempt attempt = loginAttemptRepository
                .findByEmailAndIpAddress(normalizedEmail, ipAddress)
                .orElseGet(() -> new LoginAttempt(normalizedEmail, ipAddress));

        if (isOutsideAttemptWindow(attempt, now)) {
            attempt.setFailedAttempts(0);
            attempt.setBlockedUntil(null);
        }

        int failedAttempts = attempt.getFailedAttempts() + 1;
        attempt.setFailedAttempts(failedAttempts);
        attempt.setLastFailedAttempt(now);
        if (failedAttempts >= BLOCK_THRESHOLD && failedAttempts % BLOCK_THRESHOLD == 0) {
            attempt.setBlockedUntil(now.plus(blockDuration(failedAttempts / BLOCK_THRESHOLD)));
        }

        return loginAttemptRepository.save(attempt);
    }

    public void loginSucceeded(String email, String ipAddress) {
        loginAttemptRepository.deleteByEmailAndIpAddress(normalizeEmail(email), ipAddress);
    }

    private boolean isOutsideAttemptWindow(LoginAttempt attempt, Instant now) {
        return attempt.getLastFailedAttempt() != null
                && Duration.between(attempt.getLastFailedAttempt(), now).compareTo(ATTEMPT_WINDOW) > 0;
    }

    private Duration blockDuration(int blockLevel) {
        return switch (blockLevel) {
            case 1 -> FIRST_BLOCK;
            case 2 -> SECOND_BLOCK;
            case 3 -> THIRD_BLOCK;
            default -> MAX_BLOCK;
        };
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}