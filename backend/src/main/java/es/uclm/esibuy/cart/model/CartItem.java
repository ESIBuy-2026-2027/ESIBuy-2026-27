package es.uclm.esibuy.cart.model;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.data.mongodb.core.mapping.Field;

public class CartItem {

    @Field("productoId")
    private String productId;

    @Field("nombre")
    private String name;

    @Field("precioUnitario")
    private BigDecimal unitPrice;

    private int quantity;

    private BigDecimal subtotal;

    private String imageUrl;

    protected CartItem() {
    }

    public CartItem(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            String imageUrl) {

        this.productId = Objects.requireNonNull(productId);
        this.name = Objects.requireNonNull(name);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        this.imageUrl = imageUrl;

        setQuantity(quantity);
    }

    public CartItem(String productId, String name, BigDecimal unitPrice, int quantity) {
        this(productId, name, unitPrice, quantity, null);
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    void addQuantity(int quantity) {
        setQuantity(Math.addExact(this.quantity, quantity));
    }

    void setQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad debe ser como mínimo 1");
        }

        this.quantity = quantity;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}