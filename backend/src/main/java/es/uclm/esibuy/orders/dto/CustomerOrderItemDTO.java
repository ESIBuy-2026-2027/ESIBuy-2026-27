package es.uclm.esibuy.orders.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerOrderItemDTO(
        @JsonProperty("productoId") String productId,

        @JsonProperty("nombre") String name,

        @JsonProperty("cantidad") int quantity,

        @JsonProperty("precioUnitario") BigDecimal unitPrice,

        BigDecimal subtotal,

        String imageUrl) {
}