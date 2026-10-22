
package es.uclm.esibuy.security.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "email_otp_challenges")
public class EmailOtpChallenge {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String codeHash;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private int attempts;

    private boolean used;

    protected EmailOtpChallenge() {
    }

    public EmailOtpChallenge(
            String userId,
            String codeHash,
            Instant expiresAt) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.used = false;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isUsed() {
        return used;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markUsed() {
        this.used = true;
    }
}
