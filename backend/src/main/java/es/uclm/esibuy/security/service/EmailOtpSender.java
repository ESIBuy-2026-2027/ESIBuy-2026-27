
package es.uclm.esibuy.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailOtpSender.class);

    private final boolean logOtpCode;

    public EmailOtpSender(
            @Value("${app.security.email-otp.log-code:false}") boolean logOtpCode) {
        this.logOtpCode = logOtpCode;
    }

    public void sendOtp(String email, String code) {
        if (logOtpCode) {
            LOGGER.info("[DEV ONLY] Código OTP generado: {}", code);
        }
    }
}
