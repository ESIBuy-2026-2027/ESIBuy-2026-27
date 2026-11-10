package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.UserService;

class TwoFactorAuthenticationServiceTest {

    private final UserService userService = mock(UserService.class);
    private final TotpService totpService = mock(TotpService.class);
    private final EncryptionService encryptionService = mock(EncryptionService.class);

    private final TwoFactorAuthenticationService service = new TwoFactorAuthenticationService(
            userService, totpService, encryptionService);

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(userService.findById("user-1")).thenReturn(user);
    }

    @Test
    void setupGeneratesEncryptsAndPersistsSecret() {
        when(totpService.generateSecret()).thenReturn("raw-secret");
        when(encryptionService.encrypt("raw-secret")).thenReturn("encrypted-secret");

        String result = service.setup("user-1");

        assertEquals("raw-secret", result);
        verify(user).setTotpSecret("encrypted-secret");
        verify(userService).save(user);
    }

    @Test
    void consumesAndPersistsBackupCodeAfterValidFallback() {
        List<String> backupCodes = new ArrayList<>(List.of("12345678"));
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "12345678")).thenReturn(false);
        when(user.getBackupCodes()).thenReturn(backupCodes);

        assertTrue(service.verifyLoginCode(user, " 12345678 "));

        assertTrue(backupCodes.isEmpty());
        verify(userService).save(user);
    }

    @Test
    void doesNotConsumeBackupCodeWhenTotpIsValid() {
        List<String> backupCodes = new ArrayList<>(List.of("12345678"));
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "654321")).thenReturn(true);
        when(user.getBackupCodes()).thenReturn(backupCodes);

        assertTrue(service.verifyLoginCode(user, "654321"));

        assertFalse(backupCodes.isEmpty());
        verify(userService, never()).save(user);
    }

    @Test
    void rejectsNullOrBlankLoginCode() {
        assertFalse(service.verifyLoginCode(user, null));
        assertFalse(service.verifyLoginCode(user, "   "));

        verify(encryptionService, never()).decrypt(org.mockito.ArgumentMatchers.any());
        verify(userService, never()).save(user);
    }

    @Test
    void rejectsUnknownBackupCodeWithoutPersisting() {
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "000000")).thenReturn(false);
        when(user.getBackupCodes()).thenReturn(
                new ArrayList<>(List.of("12345678")));

        assertFalse(service.verifyLoginCode(user, "000000"));

        verify(userService, never()).save(user);
    }

    @Test
    void rejectsBackupCodeWhenBackupCodeListIsNull() {
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "000000")).thenReturn(false);
        when(user.getBackupCodes()).thenReturn(null);

        assertFalse(service.verifyLoginCode(user, "000000"));

        verify(userService, never()).save(user);
    }

    @Test
    void enablesTwoFactorAndGeneratesEightBackupCodes() {
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "123456")).thenReturn(true);

        List<String> generatedCodes = List.of(
                "11111111", "22222222", "33333333", "44444444",
                "55555555", "66666666", "77777777", "88888888");

        when(totpService.generateBackupCodes(8)).thenReturn(generatedCodes);

        TwoFactorAuthenticationService.EnableResult result = service.enable("user-1", "123456");

        assertTrue(result.enabled());
        assertEquals(generatedCodes, result.backupCodes());

        verify(user).setTotpEnabled(true);
        verify(user).setBackupCodes(generatedCodes);
        verify(userService).save(user);
    }

    @Test
    void doesNotEnableTwoFactorWhenCodeIsInvalid() {
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "000000")).thenReturn(false);

        TwoFactorAuthenticationService.EnableResult result = service.enable("user-1", "000000");

        assertFalse(result.enabled());
        assertTrue(result.backupCodes().isEmpty());

        verify(totpService, never()).generateBackupCodes(8);
        verify(userService, never()).save(user);
    }

    @Test
    void doesNotEnableTwoFactorWhenCodeIsBlank() {
        TwoFactorAuthenticationService.EnableResult result = service.enable("user-1", " ");

        assertFalse(result.enabled());
        assertTrue(result.backupCodes().isEmpty());

        verify(encryptionService, never()).decrypt(org.mockito.ArgumentMatchers.any());
        verify(userService, never()).save(user);
    }

    @Test
    void doesNotEnableTwoFactorWhenSecretIsNull() {
        when(user.getTotpSecret()).thenReturn(null);

        TwoFactorAuthenticationService.EnableResult result = service.enable("user-1", "123456");

        assertFalse(result.enabled());
        verify(encryptionService, never()).decrypt(org.mockito.ArgumentMatchers.any());
        verify(userService, never()).save(user);
    }

    @Test
    void returnsDisabledWhenRegeneratingCodesWithoutTwoFactor() {
        when(user.isTotpEnabled()).thenReturn(false);

        TwoFactorAuthenticationService.BackupCodesResult result = service.regenerateBackupCodes("user-1", "123456");

        assertEquals(
                TwoFactorAuthenticationService.Status.TWO_FACTOR_DISABLED,
                result.status());
        assertTrue(result.backupCodes().isEmpty());

        verify(totpService, never()).generateBackupCodes(8);
        verify(userService, never()).save(user);
    }

    @Test
    void regeneratesBackupCodesWhenCodeIsValid() {
        when(user.isTotpEnabled()).thenReturn(true);
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "123456")).thenReturn(true);

        List<String> generatedCodes = List.of(
                "11111111", "22222222", "33333333", "44444444",
                "55555555", "66666666", "77777777", "88888888");
        when(totpService.generateBackupCodes(8)).thenReturn(generatedCodes);

        TwoFactorAuthenticationService.BackupCodesResult result = service.regenerateBackupCodes("user-1", "123456");

        assertEquals(TwoFactorAuthenticationService.Status.SUCCESS, result.status());
        assertEquals(generatedCodes, result.backupCodes());

        verify(user).setBackupCodes(generatedCodes);
        verify(userService).save(user);
    }

    @Test
    void regeneratesBackupCodesWithoutRequiringCodeWhenCodeIsNullOrBlank() {
        when(user.isTotpEnabled()).thenReturn(true);
        List<String> generatedCodes = List.of("11111111");
        when(totpService.generateBackupCodes(8)).thenReturn(generatedCodes);

        TwoFactorAuthenticationService.BackupCodesResult nullCodeResult = service.regenerateBackupCodes("user-1", null);

        assertEquals(TwoFactorAuthenticationService.Status.SUCCESS,
                nullCodeResult.status());

        TwoFactorAuthenticationService.BackupCodesResult blankCodeResult = service.regenerateBackupCodes("user-1",
                "  ");

        assertEquals(TwoFactorAuthenticationService.Status.SUCCESS,
                blankCodeResult.status());

        verify(encryptionService, never()).decrypt(org.mockito.ArgumentMatchers.any());
        verify(userService, org.mockito.Mockito.times(2)).save(user);
    }

    @Test
    void rejectsRegenerationWhenProvidedCodeIsInvalid() {
        when(user.isTotpEnabled()).thenReturn(true);
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "000000")).thenReturn(false);

        TwoFactorAuthenticationService.BackupCodesResult result = service.regenerateBackupCodes("user-1", "000000");

        assertEquals(
                TwoFactorAuthenticationService.Status.INVALID_CODE,
                result.status());
        assertTrue(result.backupCodes().isEmpty());

        verify(totpService, never()).generateBackupCodes(8);
        verify(userService, never()).save(user);
    }

    @Test
    void returnsDisabledWhenDisablingAlreadyDisabledTwoFactor() {
        when(user.isTotpEnabled()).thenReturn(false);

        TwoFactorAuthenticationService.Status result = service.disable("user-1", "123456");

        assertEquals(
                TwoFactorAuthenticationService.Status.TWO_FACTOR_DISABLED,
                result);

        verify(userService, never()).save(user);
    }

    @Test
    void rejectsDisablingWhenCodeIsInvalid() {
        when(user.isTotpEnabled()).thenReturn(true);
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "000000")).thenReturn(false);

        TwoFactorAuthenticationService.Status result = service.disable("user-1", "000000");

        assertEquals(
                TwoFactorAuthenticationService.Status.INVALID_CODE,
                result);

        verify(user, never()).setTotpEnabled(false);
        verify(userService, never()).save(user);
    }

    @Test
    void disablesTwoFactorAndClearsSecretAndBackupCodes() {
        when(user.isTotpEnabled()).thenReturn(true);
        prepareSecret("raw-secret");
        when(totpService.verifyCode("raw-secret", "123456")).thenReturn(true);

        TwoFactorAuthenticationService.Status result = service.disable("user-1", "123456");

        assertEquals(TwoFactorAuthenticationService.Status.SUCCESS, result);

        verify(user).setTotpEnabled(false);
        verify(user).setTotpSecret(null);
        verify(user).setBackupCodes(anyList());
        verify(userService).save(user);
    }

    @Test
    void rejectsDisablingWithBlankCode() {
        when(user.isTotpEnabled()).thenReturn(true);

        TwoFactorAuthenticationService.Status result = service.disable("user-1", " ");

        assertEquals(
                TwoFactorAuthenticationService.Status.INVALID_CODE,
                result);

        verify(encryptionService, never()).decrypt(org.mockito.ArgumentMatchers.any());
        verify(userService, never()).save(user);
    }

    private void prepareSecret(String rawSecret) {
        when(user.getTotpSecret()).thenReturn("encrypted-secret");
        when(encryptionService.decrypt("encrypted-secret")).thenReturn(rawSecret);
    }
}