package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private TotpService service;

    @BeforeEach
    void setUp() {
        service = new TotpService();
    }

    @Test
    void generatesNonEmptySecret() {
        String secret = service.generateSecret();

        assertNotNull(secret);
        assertFalse(secret.isBlank());
    }

    @Test
    void generatesDifferentSecrets() {
        String first = service.generateSecret();
        String second = service.generateSecret();

        assertNotNull(first);
        assertNotNull(second);
        assertFalse(first.isBlank());
        assertFalse(second.isBlank());
        assertNotEquals(first, second);
    }

    @Test
    void rejectsNullSecret() {
        assertFalse(service.verifyCode(null, "123456"));
    }

    @Test
    void rejectsNullCode() {
        assertFalse(service.verifyCode("JBSWY3DPEHPK3PXP", null));
    }

    @Test
    void rejectsInvalidCode() {
        assertFalse(service.verifyCode("JBSWY3DPEHPK3PXP", "invalid"));
    }

    @Test
    void rejectsCodeWithWrongLength() {
        assertFalse(service.verifyCode("JBSWY3DPEHPK3PXP", "123"));
    }

    @Test
    void generatesRequestedNumberOfBackupCodes() {
        List<String> codes = service.generateBackupCodes(8);

        assertEquals(8, codes.size());

        for (String code : codes) {
            assertNotNull(code);
            assertTrue(code.matches("\\d{8}"));
        }
    }

    @Test
    void generatesEmptyListWhenCountIsZero() {
        assertTrue(service.generateBackupCodes(0).isEmpty());
    }

    @Test
    void generatesEmptyListWhenCountIsNegative() {
        assertTrue(service.generateBackupCodes(-1).isEmpty());
    }

    @Test
    void generatesRequestedNumberOfBackupCodesForDifferentCounts() {
        assertEquals(1, service.generateBackupCodes(1).size());
        assertEquals(3, service.generateBackupCodes(3).size());
    }
}