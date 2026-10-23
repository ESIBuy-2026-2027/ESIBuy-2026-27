package es.uclm.esibuy.auth.passwordreset.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Introduce un correo electrónico válido")
        @Size(max = 254, message = "El correo electrónico es demasiado largo")
        String email) {
}
