package es.uclm.esibuy.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Define esta clave fija de 16 caracteres en application.properties (ej:
    // app.security.aes-key=1234567890123456)
    @Value("${app.security.aes-key:1234567890123456}")
    private String secretKey;

    public String encrypt(String value) {
        if (value == null)
            return null;
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new EncryptionException("Error cifrando el secreto TOTP", e);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null)
            return null;
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            if (decoded.length <= IV_LENGTH)
                throw new IllegalArgumentException("Texto cifrado inválido");
            byte[] iv = Arrays.copyOf(decoded, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] original = cipher.doFinal(ciphertext);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Error descifrando el secreto TOTP", e);
        }
    }

    private static final class EncryptionException extends RuntimeException {

        private EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}