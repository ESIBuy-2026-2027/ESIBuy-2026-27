package es.uclm.esibuy.orders.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.uclm.esibuy.cart.dto.CartItemDTO;
import es.uclm.esibuy.users.dto.ShippingAddressResponse;

public record CheckoutSummaryDTO(
        @JsonProperty("direcciones") List<ShippingAddressResponse> addresses,

        @JsonProperty("metodosPago") List<CheckoutPaymentMethodDTO> paymentMethods,

        List<CartItemDTO> items,

        @JsonProperty("precioTotal") BigDecimal totalPrice) {

    public CheckoutSummaryDTO {
        addresses = List.copyOf(addresses);
        paymentMethods = List.copyOf(paymentMethods);
        items = List.copyOf(items);
    }
}