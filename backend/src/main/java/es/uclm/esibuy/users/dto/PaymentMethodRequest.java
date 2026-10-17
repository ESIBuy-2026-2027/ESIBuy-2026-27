package es.uclm.esibuy.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.uclm.esibuy.users.model.PaymentMethodType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentMethodRequest(
        @NotNull(message = "El tipo de método de pago es obligatorio") PaymentMethodType type,

        @NotBlank(message = "El nombre del método es obligatorio") @Size(max = 60, message = "El nombre no puede superar los 60 caracteres") String label,

        @Size(max = 80, message = "El nombre del titular no puede superar los 80 caracteres") String cardholderName,

        @Size(max = 30, message = "La marca de la tarjeta no puede superar los 30 caracteres") String cardBrand,

        @Pattern(regexp = "^\\d{4}$", message = "Deben indicarse los últimos cuatro dígitos") String lastFourDigits,

        @Min(value = 1, message = "El mes de caducidad debe estar entre 1 y 12") @Max(value = 12, message = "El mes de caducidad debe estar entre 1 y 12") Integer expirationMonth,

        @Min(value = 2000, message = "El año de caducidad no es válido") @Max(value = 9999, message = "El año de caducidad no es válido") Integer expirationYear,

        @Email(message = "El email de PayPal no tiene un formato válido") @Size(max = 120, message = "El email de PayPal no puede superar los 120 caracteres") String paypalEmail,

        @Pattern(regexp = "^$|^\\d{9}$", message = "El teléfono de Bizum debe contener 9 dígitos") String bizumPhone,

        @JsonProperty("isDefault") boolean defaultMethod) {
}