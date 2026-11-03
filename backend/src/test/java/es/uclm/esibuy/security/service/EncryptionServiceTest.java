package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EncryptionServiceTest {

    private EncryptionService service;

    @BeforeEach
    void setUp() {
        service = new EncryptionService();
        ReflectionTestUtils.setField(
                service,
                "secretKey",
                "1234567890123456");
    }

    @Test
    void encryptsAndDecryptsTextCorrectly() {
        String original = "totp-secret-123";

        String encrypted = service.encrypt(original);

        assertNotEquals(original, encrypted);
        assertEquals(original, service.decrypt(encrypted));
    }

    @Test
    void returnsNullWhenEncryptingNull() {
        assertNull(service.encrypt(null));
    }

    @Test
    void returnsNullWhenDecryptingNull() {
        assertNull(service.decrypt(null));
    }

    @Test
    void producesDifferentCiphertextsForSamePlaintext() {
        String first = service.encrypt("same-secret");
        String second = service.encrypt("same-secret");

        assertNotEquals(first, second);
        assertEquals("same-secret", service.decrypt(first));
        assertEquals("same-secret", service.decrypt(second));
    }

    @Test
    void rejectsInvalidBase64WhenDecrypting() {
        assertThrows(
                RuntimeException.class,
                () -> service.decrypt("esto-no-es-base64!!!"));
    }

    @Test
    void rejectsCiphertextShorterThanIv() {
        String shortCiphertext = java.util.Base64.getEncoder().encodeToString(new byte[12]);

        assertThrows(
                RuntimeException.class,
                () -> service.decrypt(shortCiphertext));
    }

    @Test
    void rejectsTamperedCiphertext() {
        String encrypted = service.encrypt("sensitive-secret");

        byte[] decoded = java.util.Base64.getDecoder().decode(encrypted);
        decoded[decoded.length - 1] ^= 1;

        String tampered = java.util.Base64.getEncoder().encodeToString(decoded);

        assertThrows(
                RuntimeException.class,
                () -> service.decrypt(tampered));
    }

    @Test
    void rejectsCiphertextEncryptedWithDifferentKey() {
        String encrypted = service.encrypt("sensitive-secret");

        EncryptionService anotherService = new EncryptionService();
        ReflectionTestUtils.setField(
                anotherService,
                "secretKey",
                "abcdefghijklmnop");

        assertThrows(
                RuntimeException.class,
                () -> anotherService.decrypt(encrypted));
    }
}