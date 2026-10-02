package es.uclm.esibuy.security.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("login_attempts")
@CompoundIndex(name = "email_ip_unique", def = "{'email': 1, 'ipAddress': 1}", unique = true)
public class LoginAttempt {

    @Id
    private String id;

    private String email;
    private String ipAddress;
    private int failedAttempts;
    private Instant lastFailedAttempt;
    private Instant blockedUntil;

    protected LoginAttempt() {
    }

    public LoginAttempt(String email, String ipAddress) {
        this.email = email;
        this.ipAddress = ipAddress;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLastFailedAttempt() {
        return lastFailedAttempt;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public void setLastFailedAttempt(Instant lastFailedAttempt) {
        this.lastFailedAttempt = lastFailedAttempt;
    }

    public void setBlockedUntil(Instant blockedUntil) {
        this.blockedUntil = blockedUntil;
    }
}