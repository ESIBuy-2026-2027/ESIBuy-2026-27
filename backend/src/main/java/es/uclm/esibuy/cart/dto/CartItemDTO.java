package es.uclm.esibuy.cart.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CartItemDTO(
        @JsonProperty("productoId") String productId,

        @JsonProperty("nombre") String name,

        @JsonProperty("precioUnitario") BigDecimal unitPrice,

        @JsonProperty("cantidad") int quantity,

        BigDecimal subtotal,

        String imageUrl,

        @JsonProperty("vendedorId") String sellerId,

        @JsonProperty("vendedorNombre") String sellerName,

        @JsonProperty("precioOriginal") BigDecimal originalPrice,

        @JsonProperty("descuento") BigDecimal discount,

        @JsonProperty("esDescuentoPremium") Boolean isPremiumDiscount) {

    public CartItemDTO(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal) {

        this(productId, name, unitPrice, quantity, subtotal,
                null, null, null, unitPrice, BigDecimal.ZERO, false);
    }

    public CartItemDTO(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            String imageUrl) {

        this(productId, name, unitPrice, quantity, subtotal,
                imageUrl, null, null, unitPrice, BigDecimal.ZERO, false);
    }

    public CartItemDTO(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            String imageUrl,
            String sellerId) {

        this(productId, name, unitPrice, quantity, subtotal,
                imageUrl, sellerId, null, unitPrice, BigDecimal.ZERO, false);
    }

    public CartItemDTO(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            String imageUrl,
            String sellerId,
            String sellerName) {

        this(productId, name, unitPrice, quantity, subtotal,
                imageUrl, sellerId, sellerName, unitPrice, BigDecimal.ZERO, false);
    }

    public CartItemDTO(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            String imageUrl,
            BigDecimal originalPrice,
            BigDecimal discount,
            Boolean isPremiumDiscount) {

        this(productId, name, unitPrice, quantity, subtotal,
                imageUrl, null, null, originalPrice, discount, isPremiumDiscount);
    }
}