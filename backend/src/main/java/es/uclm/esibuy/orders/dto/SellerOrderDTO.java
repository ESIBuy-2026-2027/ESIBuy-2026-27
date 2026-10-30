package es.uclm.esibuy.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.uclm.esibuy.orders.model.OrderStatus;

public record SellerOrderDTO(
        @JsonProperty("pedidoId") String orderId,

        @JsonProperty("fechaCreacion") LocalDateTime createdAt,

        @JsonProperty("clienteNombre") String customerName,

        @JsonProperty("clienteEmail") String customerEmail,

        @JsonProperty("direccionEnvio") String shippingAddress,

        List<SellerOrderItemDTO> items,

        BigDecimal total,

        @JsonProperty("estado") OrderStatus status) {
}