package es.uclm.esibuy.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarritoDTO(
        String id,
        String clienteId,
        List<ItemCarritoDTO> items,
        BigDecimal precioTotal) {

    public CarritoDTO {
        items = List.copyOf(items);
    }
}
