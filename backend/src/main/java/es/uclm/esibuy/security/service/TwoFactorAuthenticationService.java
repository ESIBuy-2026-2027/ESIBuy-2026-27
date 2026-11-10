package es.uclm.esibuy.security.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.UserService;

/** Contains the business rules for configuring and using two-factor authentication. */
@Service
public class TwoFactorAuthenticationService {

    private static final int BACKUP_CODE_COUNT = 8;

    private final UserService userService;
    private final TotpService totpService;
    private final EncryptionService encryptionService;

    public TwoFactorAuthenticationService(
            UserService userService,
            TotpService totpService,
            EncryptionService encryptionService) {
        this.userService = userService;
        this.totpService = totpService;
        this.encryptionService = encryptionService;
    }

    public String setup(String userId) {
        User user = userService.findById(userId);
        String secret = totpService.generateSecret();
        user.setTotpSecret(encryptionService.encrypt(secret));
        userService.save(user);
        return secret;
    }

    /** Verifies a TOTP or consumes a valid one-time backup code during login. */
    public boolean verifyLoginCode(User user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        String rawSecret = encryptionService.decrypt(user.getTotpSecret());
        if (totpService.verifyCode(rawSecret, code)) {
            return true;
        }

        String backupCode = code.trim();
        List<String> backupCodes = user.getBackupCodes();
        if (backupCodes == null || !backupCodes.contains(backupCode)) {
            return false;
        }

        backupCodes.remove(backupCode);
        userService.save(user);
        return true;
    }

    public EnableResult enable(String userId, String code) {
        User user = userService.findById(userId);
        if (!verifyTotp(user, code)) {
            return new EnableResult(false, List.of());
        }

        List<String> backupCodes = totpService.generateBackupCodes(BACKUP_CODE_COUNT);
        user.setTotpEnabled(true);
        user.setBackupCodes(new ArrayList<>(backupCodes));
        userService.save(user);
        return new EnableResult(true, backupCodes);
    }

    public BackupCodesResult regenerateBackupCodes(String userId, String code) {
        User user = userService.findById(userId);
        if (!user.isTotpEnabled()) {
            return new BackupCodesResult(Status.TWO_FACTOR_DISABLED, List.of());
        }
        if (code != null && !code.isBlank() && !verifyTotp(user, code)) {
            return new BackupCodesResult(Status.INVALID_CODE, List.of());
        }

        List<String> backupCodes = totpService.generateBackupCodes(BACKUP_CODE_COUNT);
        user.setBackupCodes(new ArrayList<>(backupCodes));
        userService.save(user);
        return new BackupCodesResult(Status.SUCCESS, backupCodes);
    }

    public Status disable(String userId, String code) {
        User user = userService.findById(userId);
        if (!user.isTotpEnabled()) {
            return Status.TWO_FACTOR_DISABLED;
        }
        if (!verifyTotp(user, code)) {
            return Status.INVALID_CODE;
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setBackupCodes(new ArrayList<>());
        userService.save(user);
        return Status.SUCCESS;
    }

    private boolean verifyTotp(User user, String code) {
        if (code == null || code.isBlank() || user.getTotpSecret() == null) {
            return false;
        }
        return totpService.verifyCode(encryptionService.decrypt(user.getTotpSecret()), code);
    }

    public enum Status {
        SUCCESS,
        TWO_FACTOR_DISABLED,
        INVALID_CODE
    }

    public record EnableResult(boolean enabled, List<String> backupCodes) { }

    public record BackupCodesResult(Status status, List<String> backupCodes) { }
}
