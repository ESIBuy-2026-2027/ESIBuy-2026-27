
package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
                @NotBlank(message = "El código es obligatorio") @Pattern(regexp = "^\\d{6}$", message = "El código debe contener seis dígitos") String code) {
}
