package es.uclm.esibuy.orders.model;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.data.mongodb.core.mapping.Field;

import es.uclm.esibuy.cart.model.CartItem;

public record OrderItem(
        @Field("productoId") String productId,
        @Field("nombre") String name,
        @Field("cantidad") int quantity,
        @Field("precioUnitario") BigDecimal unitPrice,
        BigDecimal subtotal,
        String imageUrl,
        @Field("vendedorId") String sellerId) {

    public OrderItem(
            String productId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal) {
        this(productId, name, quantity, unitPrice, subtotal, null, null);
    }

    public OrderItem(
            String productId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            String imageUrl) {
        this(productId, name, quantity, unitPrice, subtotal, imageUrl, null);
    }

    public OrderItem {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(unitPrice);
        Objects.requireNonNull(subtotal);
    }

    public static OrderItem from(CartItem item) {
        Objects.requireNonNull(item);

        return new OrderItem(
                item.getProductId(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.getImageUrl(),
                null);
    }

    public OrderItem withSellerId(String sellerId) {
        return new OrderItem(
                productId,
                name,
                quantity,
                unitPrice,
                subtotal,
                imageUrl,
                sellerId);
    }
}