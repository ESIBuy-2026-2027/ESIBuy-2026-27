package es.uclm.esibuy.pedidos.model;

import java.math.BigDecimal;
import java.util.Objects;

import es.uclm.esibuy.cart.model.ItemCarrito;

public record PedidoItem(
        String productoId,
        String nombre,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {

    public PedidoItem {
        Objects.requireNonNull(productoId);
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(precioUnitario);
        Objects.requireNonNull(subtotal);
    }

    public static PedidoItem from(ItemCarrito item) {
        Objects.requireNonNull(item);
        return new PedidoItem(
                item.getProductoId(),
                item.getNombre(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal());
    }
}
