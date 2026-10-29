package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ItemPedidoVendedorDTO(
        String productoId,
        String nombre,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        String imageUrl,
        List<String> categoryIds) {
}
