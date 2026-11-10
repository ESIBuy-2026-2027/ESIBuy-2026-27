package es.uclm.esibuy.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.uclm.esibuy.orders.model.OrderPaymentStatus;
import es.uclm.esibuy.orders.model.OrderStatus;

public record CustomerOrderDTO(
                @JsonProperty("pedidoId") String orderId,

                @JsonProperty("fechaCreacion") LocalDateTime createdAt,

                List<CustomerOrderItemDTO> items,

                BigDecimal total,

                @JsonProperty("estado") OrderStatus status,

                @JsonProperty("direccionEnvio") CustomerShippingAddressDTO shippingAddress,

                @JsonProperty("metodoPago") CustomerPaymentMethodDTO paymentMethod,

                @JsonProperty("estadoPago") OrderPaymentStatus paymentStatus) {

        public CustomerOrderDTO(
                        String orderId,
                        LocalDateTime createdAt,
                        List<CustomerOrderItemDTO> items,
                        BigDecimal total,
                        OrderStatus status,
                        CustomerShippingAddressDTO shippingAddress,
                        CustomerPaymentMethodDTO paymentMethod) {
                this(
                                orderId,
                                createdAt,
                                items,
                                total,
                                status,
                                shippingAddress,
                                paymentMethod,
                                OrderPaymentStatus.PAGADO);
        }

        public CustomerOrderDTO(
                        String orderId,
                        LocalDateTime createdAt,
                        List<CustomerOrderItemDTO> items,
                        BigDecimal total,
                        OrderStatus status) {
                this(
                                orderId,
                                createdAt,
                                items,
                                total,
                                status,
                                null,
                                null,
                                OrderPaymentStatus.PAGADO);
        }
}