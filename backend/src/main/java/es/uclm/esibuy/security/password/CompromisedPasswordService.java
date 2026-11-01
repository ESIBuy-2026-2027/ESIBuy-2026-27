package es.uclm.esibuy.security.password;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CompromisedPasswordService {

    private static final int PREFIX_LENGTH = 5;

    private final HibpPasswordClient hibpPasswordClient;

    public CompromisedPasswordService(HibpPasswordClient hibpPasswordClient) {
        this.hibpPasswordClient = hibpPasswordClient;
    }

    public void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }

        String hash = calculateSha1(password);

        String prefix = hash.substring(0, PREFIX_LENGTH);
        String suffix = hash.substring(PREFIX_LENGTH);

        List<String> compromisedHashes = hibpPasswordClient.findPasswordHashes(prefix);

        boolean compromised = compromisedHashes.stream()
                .anyMatch(entry -> isCompromised(entry, suffix));

        if (compromised) {
            throw new CompromisedPasswordException();
        }
    }

    private String calculateSha1(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            byte[] hashBytes = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().withUpperCase().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "No se ha podido calcular el hash SHA-1.",
                    exception);
        }
    }

    private boolean isCompromised(String entry, String expectedSuffix) {
        String[] parts = entry.split(":", 2);

        if (parts.length != 2) {
            throw new PasswordCheckUnavailableException();
        }

        String suffix = parts[0].trim();
        String count = parts[1].trim();

        if (!suffix.matches("[A-Fa-f0-9]{35}") || !count.matches("\\d+")) {
            throw new PasswordCheckUnavailableException();
        }

        if (!suffix.equalsIgnoreCase(expectedSuffix)) {
            return false;
        }

        try {
            return Long.parseLong(count) > 0;
        } catch (NumberFormatException exception) {
            throw new PasswordCheckUnavailableException(exception);
        }
    }
}