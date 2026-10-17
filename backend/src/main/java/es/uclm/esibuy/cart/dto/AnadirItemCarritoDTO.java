package es.uclm.esibuy.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnadirItemCarritoDTO(
        @NotBlank(message = "El identificador del producto es obligatorio") String productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser como mínimo 1") Integer cantidad) {
}
