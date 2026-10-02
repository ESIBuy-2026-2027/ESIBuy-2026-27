package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(loginAttemptRepository.findByEmailAndIpAddress(any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(attempts.get(key(
                        invocation.getArgument(0), invocation.getArgument(1)))));
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> {
                    LoginAttempt attempt = invocation.getArgument(0);
                    attempts.put(key(attempt.getEmail(), attempt.getIpAddress()), attempt);
                    return attempt;
                });
    }

    @Test
    void blocksAfterFiveFailuresAndAppliesProgressiveBackoff() {
        Instant currentTime = BASE_TIME;

        for (int level = 1; level <= 4; level++) {
            for (int failure = 0; failure < 5; failure++) {
                serviceAt(currentTime.plusSeconds(failure)).loginFailed(EMAIL, IP_ADDRESS);
            }

            LoginAttemptService service = serviceAt(currentTime.plusSeconds(4));
            assertTrue(service.isBlocked(EMAIL, IP_ADDRESS));
            assertEquals(expectedBlockSeconds(level),
                    service.getRemainingBlockSeconds(EMAIL, IP_ADDRESS));
            currentTime = currentTime.plusSeconds(4 + expectedBlockSeconds(level) + 1);
        }
    }

    @Test
    void resetsCounterWhenTheAttemptWindowExpires() {
        serviceAt(BASE_TIME).loginFailed(EMAIL, IP_ADDRESS);
        serviceAt(BASE_TIME.plusSeconds(601)).loginFailed(EMAIL, IP_ADDRESS);

        LoginAttempt savedAttempt = attempts.get(key(EMAIL, IP_ADDRESS));
        assertEquals(1, savedAttempt.getFailedAttempts());
        assertFalse(serviceAt(BASE_TIME.plusSeconds(601)).isBlocked(EMAIL, IP_ADDRESS));
    }

    @Test
    void removesAttemptAfterSuccessfulLogin() {
        serviceAt(BASE_TIME).loginFailed(EMAIL, IP_ADDRESS);

        serviceAt(BASE_TIME.plusSeconds(1)).loginSucceeded(EMAIL, IP_ADDRESS);

        verify(loginAttemptRepository).deleteByEmailAndIpAddress(EMAIL, IP_ADDRESS);
    }

    private LoginAttemptService serviceAt(Instant instant) {
        return new LoginAttemptService(loginAttemptRepository, Clock.fixed(instant, ZoneOffset.UTC));
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