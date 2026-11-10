package es.uclm.esibuy.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
        @NotBlank(message = "El nombre del destinatario es obligatorio") @Size(min = 2, max = 80, message = "El nombre del destinatario debe tener entre 2 y 80 caracteres") String recipientName,

        @NotBlank(message = "La calle y el número son obligatorios") @Size(min = 3, max = 100, message = "La calle y el número deben tener entre 3 y 100 caracteres") String street,

        @Size(max = 60, message = "La información adicional no puede superar los 60 caracteres") String additionalInfo,

        @NotBlank(message = "El código postal es obligatorio") @Pattern(regexp = "^\\d{5}$", message = "El código postal debe tener exactamente 5 cifras") String postalCode,

        @NotBlank(message = "La ciudad es obligatoria") @Size(min = 2, max = 50, message = "La ciudad debe tener entre 2 y 50 caracteres") String city,

        @NotBlank(message = "La provincia es obligatoria") @Size(min = 2, max = 50, message = "La provincia debe tener entre 2 y 50 caracteres") String province,

        @Pattern(regexp = "^$|^\\d{9}$", message = "El teléfono debe contener exactamente 9 dígitos numéricos") String phone,

        @JsonProperty("isDefault") boolean defaultAddress) {
}
