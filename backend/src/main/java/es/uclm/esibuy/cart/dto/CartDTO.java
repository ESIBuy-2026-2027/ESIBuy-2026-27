package es.uclm.esibuy.cart.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CartDTO(
                String id,

                @JsonProperty("clienteId") String customerId,

                List<CartItemDTO> items,

                @JsonProperty("precioTotal") BigDecimal totalPrice,

                @JsonProperty("totalSinDescuento") BigDecimal totalBeforeDiscount,

                @JsonProperty("descuentoTotal") BigDecimal totalDiscount) {

        public CartDTO {
                items = List.copyOf(items);
        }

        public CartDTO(
                        String id,
                        String customerId,
                        List<CartItemDTO> items,
                        BigDecimal totalPrice) {

                this(id, customerId, items, totalPrice, totalPrice, BigDecimal.ZERO);
        }
}