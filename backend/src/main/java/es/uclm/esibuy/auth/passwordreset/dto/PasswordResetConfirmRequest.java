package es.uclm.esibuy.auth.passwordreset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "El token es obligatorio")
        @Size(max = 128, message = "El token no es válido")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 12, max = 128, message = "La contraseña debe tener entre 12 y 128 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$",
                message = "La contraseña debe incluir mayúscula, minúscula, número y carácter especial, sin espacios")
        String newPassword) {
}
