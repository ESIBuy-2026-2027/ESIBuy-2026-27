package es.uclm.esibuy.auth.passwordreset.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordResetRateLimiterTest {

    private static final String EMAIL = "usuario@example.com";

    private PasswordResetRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new PasswordResetRateLimiter();
    }

    @Test
    void shouldAllowThreeRequestsFromSameIpWithinWindow() {
        assertTrue(rateLimiter.allow("192.168.1.10", "uno@example.com"));
        assertTrue(rateLimiter.allow("192.168.1.10", "dos@example.com"));
        assertTrue(rateLimiter.allow("192.168.1.10", "tres@example.com"));
    }

    @Test
    void shouldRejectFourthRequestFromSameIpWithinWindow() {
        assertTrue(rateLimiter.allow("192.168.1.10", "uno@example.com"));
        assertTrue(rateLimiter.allow("192.168.1.10", "dos@example.com"));
        assertTrue(rateLimiter.allow("192.168.1.10", "tres@example.com"));

        assertFalse(rateLimiter.allow("192.168.1.10", "cuatro@example.com"));
    }

    @Test
    void shouldLimitRequestsPerEmailEvenWhenTheyComeFromDifferentIps() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertTrue(rateLimiter.allow(
                    "192.168.1." + attempt,
                    EMAIL));
        }

        assertFalse(rateLimiter.allow("192.168.1.20", EMAIL));
    }

    @Test
    void shouldApplyIpAndEmailLimitsIndependently() {
        PasswordResetRateLimiter ipLimiter = new PasswordResetRateLimiter();

        assertTrue(ipLimiter.allow("192.168.1.10", "uno@example.com"));
        assertTrue(ipLimiter.allow("192.168.1.10", "dos@example.com"));
        assertTrue(ipLimiter.allow("192.168.1.10", "tres@example.com"));

        assertFalse(ipLimiter.allow("192.168.1.10", "cuatro@example.com"));

        PasswordResetRateLimiter emailLimiter = new PasswordResetRateLimiter();

        for (int attempt = 0; attempt < 5; attempt++) {
            assertTrue(emailLimiter.allow(
                    "192.168.2." + attempt,
                    EMAIL));
        }

        assertFalse(emailLimiter.allow("192.168.2.20", EMAIL));
    }

    @Test
    void shouldUseUnknownKeyWhenIpAddressIsNull() {
        assertTrue(rateLimiter.allow(null, "uno@example.com"));
        assertTrue(rateLimiter.allow(null, "dos@example.com"));
        assertTrue(rateLimiter.allow(null, "tres@example.com"));

        assertFalse(rateLimiter.allow(null, "cuatro@example.com"));
    }
}