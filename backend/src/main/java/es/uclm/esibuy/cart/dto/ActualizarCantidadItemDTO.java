package es.uclm.esibuy.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarCantidadItemDTO(
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad debe ser como mínimo 0") Integer cantidad) {
}
