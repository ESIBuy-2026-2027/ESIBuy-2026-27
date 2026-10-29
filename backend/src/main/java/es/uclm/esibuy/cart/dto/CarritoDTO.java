package es.uclm.esibuy.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarritoDTO(
        String id,
        String clienteId,
        List<ItemCarritoDTO> items,
        BigDecimal precioTotal,
        BigDecimal totalSinDescuento,
        BigDecimal descuentoTotal) {

    public CarritoDTO {
        items = List.copyOf(items);
    }

    public CarritoDTO(
            String id,
            String clienteId,
            List<ItemCarritoDTO> items,
            BigDecimal precioTotal) {
        this(id, clienteId, items, precioTotal, precioTotal, BigDecimal.ZERO);
    }
}
