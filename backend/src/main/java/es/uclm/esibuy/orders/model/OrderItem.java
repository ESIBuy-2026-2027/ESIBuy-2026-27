package es.uclm.esibuy.orders.model;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.data.mongodb.core.mapping.Field;

import es.uclm.esibuy.cart.model.ItemCarrito;

public record OrderItem(
        @Field("productoId") String productId,
        @Field("nombre") String name,
        @Field("cantidad") int quantity,
        @Field("precioUnitario") BigDecimal unitPrice,
        BigDecimal subtotal,
        String imageUrl) {

    public OrderItem(
            String productId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal) {
        this(productId, name, quantity, unitPrice, subtotal, null);
    }

    public OrderItem {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(unitPrice);
        Objects.requireNonNull(subtotal);
    }

    public static OrderItem from(ItemCarrito item) {
        Objects.requireNonNull(item);

        return new OrderItem(
                item.getProductoId(),
                item.getNombre(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal(),
                item.getImageUrl());
    }
}