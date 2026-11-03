package es.uclm.esibuy.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.uclm.esibuy.orders.model.OrderPaymentStatus;
import es.uclm.esibuy.orders.model.OrderStatus;

public record OrderConfirmationDTO(
        @JsonProperty("pedidoId") String orderId,

        @JsonProperty("precioTotal") BigDecimal totalPrice,

        @JsonProperty("estado") OrderStatus status,

        @JsonProperty("fechaCreacion") LocalDateTime createdAt,

        @JsonProperty("estadoPago") OrderPaymentStatus paymentStatus) {

    public OrderConfirmationDTO(
            String orderId,
            BigDecimal totalPrice,
            OrderStatus status,
            LocalDateTime createdAt) {
        this(
                orderId,
                totalPrice,
                status,
                createdAt,
                OrderPaymentStatus.PAGADO);
    }
}