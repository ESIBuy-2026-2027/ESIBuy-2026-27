package es.uclm.esibuy.security.password;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PepperedPasswordEncoderTest {

    private static final String PASSWORD = "Password123!";

    @Test
    void encodesAndMatchesWithPepperVersion() {
        PepperedPasswordEncoder encoder = new PepperedPasswordEncoder("test-pepper");

        String encoded = encoder.encode(PASSWORD);

        assertTrue(encoded.startsWith(PepperedPasswordEncoder.VERSION_PREFIX));
        assertTrue(encoder.matches(PASSWORD, encoded));
        assertFalse(encoder.matches("wrong", encoded));
        assertFalse(encoder.upgradeEncoding(encoded));
    }

    @Test
    void producesDifferentHashesForSamePassword() {
        PepperedPasswordEncoder encoder = new PepperedPasswordEncoder("test-pepper");

        assertFalse(encoder.encode(PASSWORD).equals(encoder.encode(PASSWORD)));
    }

    @Test
    void rejectsDifferentPepper() {
        String encoded = new PepperedPasswordEncoder("test-pepper").encode(PASSWORD);

        assertFalse(new PepperedPasswordEncoder("other-pepper").matches(PASSWORD, encoded));
    }

    @Test
    void matchesLegacyBcryptAndMarksItForUpgrade() {
        String legacyHash = new BCryptPasswordEncoder().encode(PASSWORD);
        PepperedPasswordEncoder encoder = new PepperedPasswordEncoder("test-pepper");

        assertTrue(encoder.matches(PASSWORD, legacyHash));
        assertTrue(encoder.upgradeEncoding(legacyHash));
    }

    @Test
    void rejectsUnknownOrInvalidFormats() {
        PepperedPasswordEncoder encoder = new PepperedPasswordEncoder("test-pepper");

        assertFalse(encoder.matches(PASSWORD, "unknown-format"));
        assertFalse(encoder.matches(PASSWORD, PepperedPasswordEncoder.VERSION_PREFIX + "invalid"));
        assertFalse(encoder.upgradeEncoding("unknown-format"));
    }

    @Test
    void rejectsMissingOrBlankPepper() {
        assertThrows(IllegalArgumentException.class, () -> new PepperedPasswordEncoder(null));
        assertThrows(IllegalArgumentException.class, () -> new PepperedPasswordEncoder(""));
        assertThrows(IllegalArgumentException.class, () -> new PepperedPasswordEncoder("   "));
    }
}
