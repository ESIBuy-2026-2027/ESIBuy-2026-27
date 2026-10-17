package es.uclm.esibuy.cart.dto;

import java.math.BigDecimal;

public record ItemCarritoDTO(
        String productoId,
        String nombre,
        BigDecimal precioUnitario,
        int cantidad,
        BigDecimal subtotal) {
}
