
package es.uclm.esibuy.security.service;

import org.springframework.stereotype.Service;

@Service
public class OtpMailSender {

    public void sendOtp(String email, String code) {
        // TODO: sustituir por el envío mediante la API de Brevo.
        // Solo para desarrollo local: no mantener el OTP en logs
        // cuando se integre el envío real.
        System.out.printf(
                "[DEV ONLY] OTP para %s: %s%n",
                email,
                code);
    }
}
