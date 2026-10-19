package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.UserService;

class TwoFactorAuthenticationServiceTest {

    private final UserService userService = mock(UserService.class);
    private final TotpService totpService = mock(TotpService.class);
    private final EncryptionService encryptionService = mock(EncryptionService.class);
    private final TwoFactorAuthenticationService service = new TwoFactorAuthenticationService(
            userService, totpService, encryptionService);

    @Test
    void consumesAndPersistsBackupCodeAfterValidFallback() {
        User user = mock(User.class);
        List<String> backupCodes = new ArrayList<>(List.of("12345678"));
        when(user.getTotpSecret()).thenReturn("encrypted-secret");
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(totpService.verifyCode("raw-secret", "12345678")).thenReturn(false);
        when(user.getBackupCodes()).thenReturn(backupCodes);

        assertTrue(service.verifyLoginCode(user, " 12345678 "));

        assertTrue(backupCodes.isEmpty());
        verify(userService).save(user);
    }

    @Test
    void doesNotConsumeBackupCodeWhenTotpIsValid() {
        User user = mock(User.class);
        List<String> backupCodes = new ArrayList<>(List.of("12345678"));
        when(user.getTotpSecret()).thenReturn("encrypted-secret");
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(totpService.verifyCode("raw-secret", "654321")).thenReturn(true);
        when(user.getBackupCodes()).thenReturn(backupCodes);

        assertTrue(service.verifyLoginCode(user, "654321"));

        assertFalse(backupCodes.isEmpty());
        verify(userService, never()).save(user);
    }

    @Test
    void rejectsUnknownBackupCodeWithoutPersisting() {
        User user = mock(User.class);
        when(user.getTotpSecret()).thenReturn("encrypted-secret");
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(totpService.verifyCode("raw-secret", "000000")).thenReturn(false);
        when(user.getBackupCodes()).thenReturn(new ArrayList<>(List.of("12345678")));

        assertFalse(service.verifyLoginCode(user, "000000"));
        verify(userService, never()).save(user);
    }
}
