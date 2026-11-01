package es.uclm.esibuy.security.password;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PepperedPasswordEncoder implements PasswordEncoder {

    public static final String VERSION_PREFIX = "{bcrypt-pepper-v1}";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String BCRYPT_PREFIX = "$2";

    private final byte[] pepper;
    private final BCryptPasswordEncoder bcrypt;

    public PepperedPasswordEncoder(String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalArgumentException("Password pepper must not be null or blank");
        }
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
        this.bcrypt = new BCryptPasswordEncoder();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        return VERSION_PREFIX + bcrypt.encode(pepper(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        try {
            if (encodedPassword.startsWith(VERSION_PREFIX)) {
                String bcryptHash = encodedPassword.substring(VERSION_PREFIX.length());
                return isBcryptHash(bcryptHash) && bcrypt.matches(pepper(rawPassword), bcryptHash);
            }
            return isBcryptHash(encodedPassword) && bcrypt.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return isBcryptHash(encodedPassword);
    }

    private String pepper(CharSequence rawPassword) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(
                    mac.doFinal(rawPassword.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to initialize password HMAC", exception);
        }
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith(BCRYPT_PREFIX) && value.length() == 60;
    }

}
