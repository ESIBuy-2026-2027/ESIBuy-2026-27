package es.uclm.esibuy.orders.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SellerOrderItemDTO(
        @JsonProperty("productoId") String productId,

        @JsonProperty("nombre") String name,

        @JsonProperty("cantidad") int quantity,

        @JsonProperty("precioUnitario") BigDecimal unitPrice,

        BigDecimal subtotal,

        String imageUrl,

        @JsonProperty("categoryIds") List<String> categoryIds) {
}