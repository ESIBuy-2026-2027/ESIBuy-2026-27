package es.uclm.esibuy.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemQuantityDTO(
                @NotNull(message = "La cantidad es obligatoria") @Min(value = 0, message = "La cantidad debe ser como mínimo 0") @JsonProperty("cantidad") Integer quantity) {
}