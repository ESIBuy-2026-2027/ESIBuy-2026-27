
package es.uclm.esibuy.auth.passwordreset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailServiceTest {

    private static final String FROM = "no-reply@esibuy.com";
    private static final String RECIPIENT = "usuario@example.com";

    private static final String RESET_URL = "https://esibuy.example/restablecer-password?token=abc123";

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetEmailService emailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        emailService = new PasswordResetEmailService(mailSender, FROM);
        mimeMessage = new MimeMessage(SessionProvider.create());
    }

    @Test
    void shouldSendMultipartEmailWithExpectedContent() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail(RECIPIENT, RESET_URL);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();

        assertNotNull(sentMessage);

        assertEquals(
                "Recuperación de contraseña | ESIBuy",
                sentMessage.getSubject());

        assertTrue(sentMessage.getAllRecipients()[0]
                .toString().contains(RECIPIENT));

        assertTrue(sentMessage.getFrom()[0]
                .toString().contains(FROM));

        Object content = sentMessage.getContent();

        assertTrue(
                content instanceof Multipart,
                "El contenido del correo debe ser multipart");

        Multipart multipart = (Multipart) content;

        assertTrue(
                multipart.getCount() > 0,
                "El mensaje debe contener al menos una parte MIME");
    }

    @Test
    void shouldGeneratePlainTextWithExpectedContent() {
        String plainText = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildPlainText",
                RESET_URL);

        assertNotNull(plainText);
        assertTrue(plainText.contains("ESIBuy"));
        assertTrue(plainText.contains("15 minutos"));
        assertTrue(plainText.contains(RESET_URL));
        assertFalse(plainText.contains("__RESET_URL__"));
    }

    @Test
    void shouldGenerateHtmlWithExpectedContent() {
        String html = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildHtml",
                RESET_URL);

        assertNotNull(html);
        assertTrue(html.contains("ESIBuy"));
        assertTrue(html.contains("15 minutos"));
        assertTrue(html.contains(RESET_URL));
        assertFalse(html.contains("__RESET_URL__"));
    }

    @Test
    void shouldEscapeHtmlCharactersInResetUrl() {
        String urlWithHtml = "https://example.com/reset?a=1&b=<script>";

        String html = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildHtml",
                urlWithHtml);

        assertNotNull(html);

        assertTrue(html.contains("&amp;"),
                "El carácter & debe escaparse como &amp;");

        assertTrue(html.contains("&lt;script&gt;"),
                "Las etiquetas HTML deben escaparse");

        assertFalse(html.contains("<script>"),
                "La URL no debe insertar etiquetas HTML sin escapar");

        assertFalse(html.contains("__RESET_URL__"),
                "El HTML no debe conservar el marcador de sustitución");
    }

    @Test
    void shouldWrapMessagePreparationErrorsAsMailSendException()
            throws Exception {

        MimeMessage messageWithError = org.mockito.Mockito.spy(mimeMessage);

        when(mailSender.createMimeMessage()).thenReturn(messageWithError);

        doThrow(new MessagingException("Error preparando el remitente"))
                .when(messageWithError)
                .setFrom(org.mockito.ArgumentMatchers.any(
                        jakarta.mail.Address.class));

        assertThrows(
                MailSendException.class,
                () -> emailService.sendPasswordResetEmail(
                        RECIPIENT,
                        RESET_URL));
    }

    @Test
    void shouldPropagateMailSendingErrors() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        doThrow(new MailSendException("SMTP no disponible"))
                .when(mailSender)
                .send(mimeMessage);

        assertThrows(
                MailSendException.class,
                () -> emailService.sendPasswordResetEmail(
                        RECIPIENT,
                        RESET_URL));
    }

    private static final class SessionProvider {

        private SessionProvider() {
        }

        private static jakarta.mail.Session create() {
            return jakarta.mail.Session.getInstance(new Properties());
        }
    }
}
