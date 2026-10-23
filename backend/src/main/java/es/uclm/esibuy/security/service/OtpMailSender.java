
package es.uclm.esibuy.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtpMailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpMailSender.class);

    public void sendOtp(String email, String code) {
        // TODO: sustituir por el envío mediante la API de Brevo.
        // Solo para desarrollo local: no mantener el OTP en logs
        // cuando se integre el envío real.
        LOGGER.debug(
                "[DEV ONLY] Solicitud de envío de OTP para {}. "
                        + "El código es: {}.",
                email,
                code);
    }
}
