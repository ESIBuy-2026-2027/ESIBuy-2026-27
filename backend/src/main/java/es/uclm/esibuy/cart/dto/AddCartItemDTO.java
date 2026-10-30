package es.uclm.esibuy.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCartItemDTO(
                @NotBlank(message = "El identificador del producto es obligatorio") @JsonProperty("productoId") String productId,

                @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser como mínimo 1") @JsonProperty("cantidad") Integer quantity) {
}