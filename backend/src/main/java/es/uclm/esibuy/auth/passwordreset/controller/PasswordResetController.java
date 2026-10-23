package es.uclm.esibuy.auth.passwordreset.controller;

import java.util.Map;

import es.uclm.esibuy.auth.passwordreset.dto.PasswordResetConfirmRequest;
import es.uclm.esibuy.auth.passwordreset.dto.PasswordResetRequest;
import es.uclm.esibuy.auth.passwordreset.dto.PasswordResetTokenRequest;
import es.uclm.esibuy.auth.passwordreset.service.PasswordResetService;
import es.uclm.esibuy.auth.passwordreset.service.PasswordResetService.InvalidPasswordResetTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private static final String MESSAGE_KEY = "message";

    private static final String REQUEST_MESSAGE = "Si el correo está registrado, recibirás instrucciones para recuperar tu contraseña.";

    private final PasswordResetService passwordResetService;

    public PasswordResetController(
            PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest servletRequest) {

        passwordResetService.requestReset(
                request.email(),
                servletRequest.getRemoteAddr());

        return ResponseEntity.ok(
                Map.of(MESSAGE_KEY, REQUEST_MESSAGE));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, String>> validate(
            @Valid @RequestBody PasswordResetTokenRequest request) {

        passwordResetService.validateResetToken(request.token());

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY,
                "El código es válido. Ya puedes establecer una nueva contraseña."));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(
            @Valid @RequestBody PasswordResetConfirmRequest request) {

        passwordResetService.confirmReset(
                request.token(),
                request.newPassword());

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY,
                "La contraseña se ha actualizado correctamente. Ya puedes iniciar sesión."));
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(
            InvalidPasswordResetTokenException exception) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(MESSAGE_KEY, exception.getMessage()));
    }
}