
package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailOtpDisableRequest(
        @NotBlank(message = "Introduce el código de verificación") @Pattern(regexp = "\\d{6}", message = "El código debe contener 6 cifras") String code) {
}
