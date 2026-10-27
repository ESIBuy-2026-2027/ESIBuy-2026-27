package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;

public record ItemPedidoClienteDTO(
        String productoId,
        String nombre,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        String imageUrl) {
}
