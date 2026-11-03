package es.uclm.esibuy.orders.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderDTO(
        @NotBlank(message = "La dirección de envío es obligatoria") @JsonProperty("direccionId") String shippingAddressId,

        @NotBlank(message = "El método de pago es obligatorio") @JsonProperty("metodoPagoId") String paymentMethodId,

        @JsonProperty("vendedorId") String sellerId,

        @JsonProperty("productoIds") List<String> productIds) {

    public CreateOrderDTO(String shippingAddressId, String paymentMethodId) {
        this(shippingAddressId, paymentMethodId, null, null);
    }

    public CreateOrderDTO(
            String shippingAddressId,
            String paymentMethodId,
            String sellerId) {
        this(shippingAddressId, paymentMethodId, sellerId, null);
    }
}