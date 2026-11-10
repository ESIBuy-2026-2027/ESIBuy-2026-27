package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import es.uclm.esibuy.security.model.LoginAttempt;
import es.uclm.esibuy.security.repository.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String IP_ADDRESS = "192.0.2.10";
    private static final Instant BASE_TIME = Instant.parse("2026-10-02T10:00:00Z");

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    private final Map<String, LoginAttempt> attempts = new HashMap<>();

    @BeforeEach
    void setUpRepository() {
        attempts.clear();

        lenient().when(loginAttemptRepository.findByEmailAndIpAddress(any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        attempts.get(key(
                                invocation.getArgument(0),
                                invocation.getArgument(1)))));

        lenient().when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> {
                    LoginAttempt attempt = invocation.getArgument(0);
                    attempts.put(key(attempt.getEmail(), attempt.getIpAddress()), attempt);
                    return attempt;
                });
    }

    @Test
    void returnsZeroWhenThereIsNoPreviousAttempt() {
        LoginAttemptService service = serviceAt(BASE_TIME);

        assertFalse(service.isBlocked(EMAIL, IP_ADDRESS));
        assertEquals(0, service.getRemainingBlockSeconds(EMAIL, IP_ADDRESS));
    }

    @Test
    void blocksAfterFiveFailuresAndAppliesProgressiveBackoff() {
        Instant currentTime = BASE_TIME;

        for (int level = 1; level <= 4; level++) {
            for (int failure = 0; failure < 5; failure++) {
                serviceAt(currentTime.plusSeconds(failure))
                        .loginFailed(EMAIL, IP_ADDRESS);
            }

            Instant blockTime = currentTime.plusSeconds(4);
            LoginAttemptService service = serviceAt(blockTime);

            assertTrue(service.isBlocked(EMAIL, IP_ADDRESS));
            assertEquals(expectedBlockSeconds(level),
                    service.getRemainingBlockSeconds(EMAIL, IP_ADDRESS));

            currentTime = blockTime.plusSeconds(expectedBlockSeconds(level) + 1);
        }
    }

    @Test
    void returnsZeroWhenBlockHasExpired() {
        LoginAttempt attempt = new LoginAttempt(EMAIL, IP_ADDRESS);
        attempt.setBlockedUntil(BASE_TIME.minusSeconds(1));
        attempts.put(key(EMAIL, IP_ADDRESS), attempt);

        LoginAttemptService service = serviceAt(BASE_TIME);

        assertFalse(service.isBlocked(EMAIL, IP_ADDRESS));
        assertEquals(0, service.getRemainingBlockSeconds(EMAIL, IP_ADDRESS));
    }

    @Test
    void roundsRemainingBlockTimeUpToWholeSeconds() {
        LoginAttempt attempt = new LoginAttempt(EMAIL, IP_ADDRESS);
        attempt.setBlockedUntil(BASE_TIME.plusMillis(1500));
        attempts.put(key(EMAIL, IP_ADDRESS), attempt);

        assertEquals(2,
                serviceAt(BASE_TIME).getRemainingBlockSeconds(EMAIL, IP_ADDRESS));
    }

    @Test
    void resetsCounterWhenAttemptWindowExpires() {
        serviceAt(BASE_TIME).loginFailed(EMAIL, IP_ADDRESS);
        serviceAt(BASE_TIME.plusSeconds(601)).loginFailed(EMAIL, IP_ADDRESS);

        LoginAttempt savedAttempt = attempts.get(key(EMAIL, IP_ADDRESS));

        assertEquals(1, savedAttempt.getFailedAttempts());
        assertNull(savedAttempt.getBlockedUntil());
    }

    @Test
    void doesNotResetCounterAtExactAttemptWindowBoundary() {
        serviceAt(BASE_TIME).loginFailed(EMAIL, IP_ADDRESS);
        serviceAt(BASE_TIME.plusSeconds(600)).loginFailed(EMAIL, IP_ADDRESS);

        LoginAttempt savedAttempt = attempts.get(key(EMAIL, IP_ADDRESS));

        assertEquals(2, savedAttempt.getFailedAttempts());
    }

    @Test
    void normalizesEmailBeforeFindingAndSavingAttempt() {
        serviceAt(BASE_TIME).loginFailed("  USER@EXAMPLE.COM  ", IP_ADDRESS);

        assertTrue(attempts.containsKey(key(EMAIL, IP_ADDRESS)));
        assertEquals(1, attempts.get(key(EMAIL, IP_ADDRESS)).getFailedAttempts());
    }

    @Test
    void removesAttemptAfterSuccessfulLoginUsingNormalizedEmail() {
        serviceAt(BASE_TIME).loginFailed(EMAIL, IP_ADDRESS);

        serviceAt(BASE_TIME.plusSeconds(1))
                .loginSucceeded("  USER@EXAMPLE.COM  ", IP_ADDRESS);

        verify(loginAttemptRepository)
                .deleteByEmailAndIpAddress(EMAIL, IP_ADDRESS);
    }

    @Test
    void keepsAttemptsSeparateForDifferentIpAddresses() {
        serviceAt(BASE_TIME).loginFailed(EMAIL, IP_ADDRESS);
        serviceAt(BASE_TIME).loginFailed(EMAIL, "192.0.2.20");

        assertEquals(1, attempts.get(key(EMAIL, IP_ADDRESS)).getFailedAttempts());
        assertEquals(1, attempts.get(key(EMAIL, "192.0.2.20")).getFailedAttempts());
    }

    private LoginAttemptService serviceAt(Instant instant) {
        return new LoginAttemptService(
                loginAttemptRepository,
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private long expectedBlockSeconds(int level) {
        return switch (level) {
            case 1 -> 30;
            case 2 -> 120;
            case 3 -> 240;
            default -> 300;
        };
    }

    private String key(String email, String ipAddress) {
        return email + "|" + ipAddress;
    }
}