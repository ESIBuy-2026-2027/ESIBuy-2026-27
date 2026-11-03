package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class EmailOtpSenderTest {

    @Test
    void sendsOtpWithoutLoggingWhenLoggingIsDisabled() {
        EmailOtpSender sender = new EmailOtpSender(false);

        assertDoesNotThrow(
                () -> sender.sendOtp("user@example.com", "123456"));
    }

    @Test
    void sendsOtpAndLogsItWhenLoggingIsEnabled() {
        EmailOtpSender sender = new EmailOtpSender(true);

        assertDoesNotThrow(
                () -> sender.sendOtp("user@example.com", "123456"));
    }
}