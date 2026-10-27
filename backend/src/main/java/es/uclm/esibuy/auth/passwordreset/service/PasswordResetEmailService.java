
package es.uclm.esibuy.auth.passwordreset.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private static final String BRAND_NAME = "ESIBuy";
    private static final String SUBJECT = "Recuperación de contraseña | ESIBuy";
    private static final String RESET_URL_PLACEHOLDER = "__RESET_URL__";

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public PasswordResetEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetEmail(
            String recipient,
            String resetUrl) {

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress, BRAND_NAME);
            helper.setTo(recipient);
            helper.setSubject(SUBJECT);
            helper.setText(
                    buildPlainText(resetUrl),
                    buildHtml(resetUrl));

        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailSendException(
                    "No se pudo preparar el correo de recuperación.",
                    exception);
        }

        mailSender.send(message);
    }

    private String buildHtml(String resetUrl) {
        String safeUrl = escapeHtml(resetUrl);

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light">
                    <title>Recuperación de contraseña | ESIBuy</title>
                </head>
                <body style="margin:0;padding:0;background-color:#edf3fa;
                             font-family:Arial,Helvetica,sans-serif;color:#102c40;">

                    <div style="display:none;max-height:0;overflow:hidden;
                                opacity:0;color:transparent;">
                        Recupera el acceso a tu cuenta de ESIBuy de forma segura.
                    </div>

                    <table role="presentation" width="100%" cellspacing="0"
                           cellpadding="0" border="0"
                           style="background-color:#edf3fa;width:100%;">
                        <tr>
                            <td align="center" style="padding:32px 14px;">

                                <table role="presentation" width="560"
                                       cellspacing="0" cellpadding="0" border="0"
                                       style="width:100%;max-width:560px;
                                              border-collapse:separate;
                                              border-spacing:0;">

                                    <tr>
                                        <td align="center"
                                            style="background-color:#102c40;
                                                   padding:28px 20px;
                                                   border-radius:14px 14px 0 0;">

                                            <div style="font-size:32px;
                                                        font-weight:800;
                                                        letter-spacing:-1px;
                                                        line-height:1.2;">
                                                <span style="color:#ffffff;">ESI</span><span
                                                    style="color:#42d6b2;">Buy</span>
                                            </div>

                                            <p style="margin:9px 0 0;
                                                      color:#dce8f2;
                                                      font-size:13px;
                                                      line-height:1.5;">
                                                Tu espacio de confianza
                                            </p>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="background-color:#ffffff;
                                                   padding:36px 32px 28px;">

                                            <p style="margin:0 0 12px;
                                                      color:#168a75;
                                                      font-size:11px;
                                                      font-weight:bold;
                                                      letter-spacing:1.5px;
                                                      text-transform:uppercase;">
                                                Recuperación de cuenta
                                            </p>

                                            <h1 style="margin:0 0 22px;
                                                       color:#102c40;
                                                       font-size:27px;
                                                       line-height:1.3;
                                                       font-weight:700;">
                                                ¿Has olvidado tu contraseña?
                                            </h1>

                                            <p style="margin:0 0 16px;
                                                      color:#42586a;
                                                      font-size:15px;
                                                      line-height:1.8;">
                                                Hola,
                                            </p>

                                            <p style="margin:0 0 16px;
                                                      color:#42586a;
                                                      font-size:15px;
                                                      line-height:1.8;">
                                                Hemos recibido una solicitud para
                                                restablecer la contraseña de tu
                                                cuenta de <strong style="color:#102c40;">ESIBuy</strong>.
                                            </p>

                                            <p style="margin:0 0 26px;
                                                      color:#42586a;
                                                      font-size:15px;
                                                      line-height:1.8;">
                                                Pulsa el siguiente botón para
                                                elegir una nueva contraseña y
                                                recuperar el acceso a tu cuenta.
                                            </p>

                                            <table role="presentation" width="100%"
                                                   cellspacing="0" cellpadding="0"
                                                   border="0">
                                                <tr>
                                                    <td align="center"
                                                        style="padding:0 0 28px;">
                                                        <a href="__RESET_URL__"
                                                           style="display:inline-block;
                                                                  background-color:#42d6b2;
                                                                  color:#102c40;
                                                                  padding:15px 28px;
                                                                  border-radius:8px;
                                                                  font-size:15px;
                                                                  line-height:1.3;
                                                                  font-weight:bold;
                                                                  text-decoration:none;">
                                                            Restablecer contraseña
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <table role="presentation" width="100%"
                                                   cellspacing="0" cellpadding="0"
                                                   border="0">
                                                <tr>
                                                    <td style="background-color:#edf3fa;
                                                               border-left:4px solid #42d6b2;
                                                               padding:15px 17px;
                                                               border-radius:4px;">
                                                        <p style="margin:0;
                                                                  color:#102c40;
                                                                  font-size:13px;
                                                                  line-height:1.7;">
                                                            <strong>Por tu seguridad</strong><br>
                                                            Este enlace caduca en
                                                            <strong>15 minutos</strong>
                                                            y solo puede utilizarse
                                                            una vez.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin:26px 0 10px;
                                                      color:#42586a;
                                                      font-size:13px;
                                                      line-height:1.7;">
                                                ¿No funciona el botón? Copia y pega
                                                esta dirección en tu navegador:
                                            </p>

                                            <p style="margin:0;
                                                      font-size:12px;
                                                      line-height:1.8;
                                                      overflow-wrap:anywhere;
                                                      word-break:break-word;">
                                                <a href="__RESET_URL__"
                                                   style="color:#168a75;
                                                          text-decoration:underline;
                                                          overflow-wrap:anywhere;
                                                          word-break:break-word;">
                                                    __RESET_URL__
                                                </a>
                                            </p>

                                            <hr style="border:0;
                                                       border-top:1px solid #e9eff8;
                                                       margin:28px 0 22px;">

                                            <p style="margin:0;
                                                      color:#617587;
                                                      font-size:13px;
                                                      line-height:1.8;">
                                                Si no has solicitado este cambio,
                                                puedes ignorar este correo.
                                                Tu contraseña seguirá siendo la
                                                misma mientras no completes el
                                                proceso de recuperación.
                                            </p>

                                        </td>
                                    </tr>

                                    <tr>
                                        <td align="center"
                                            style="background-color:#f7f9fc;
                                                   padding:22px 24px;
                                                   border-radius:0 0 14px 14px;
                                                   border-top:1px solid #e1eaf0;">

                                            <p style="margin:0 0 7px;
                                                      color:#102c40;
                                                      font-size:13px;
                                                      font-weight:bold;">
                                                ESIBuy
                                            </p>

                                            <p style="margin:0;
                                                      color:#718295;
                                                      font-size:11px;
                                                      line-height:1.8;">
                                                Plataforma de compraventa<br>
                                                Este es un mensaje automático.
                                                No respondas a este correo.
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.replace(RESET_URL_PLACEHOLDER, safeUrl);
    }

    private String buildPlainText(String resetUrl) {
        return """
                ESIBuy | Recuperación de contraseña

                Hola,

                Hemos recibido una solicitud para restablecer la contraseña
                de tu cuenta de ESIBuy.

                Para elegir una nueva contraseña, abre este enlace:

                __RESET_URL__

                Por tu seguridad, el enlace caduca en 15 minutos y solo
                puede utilizarse una vez.

                Si no has solicitado este cambio, puedes ignorar este correo.
                Tu contraseña seguirá siendo la misma mientras no completes
                el proceso de recuperación.

                ESIBuy
                Este es un mensaje automático. No respondas a este correo.
                """.replace(RESET_URL_PLACEHOLDER, resetUrl);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
