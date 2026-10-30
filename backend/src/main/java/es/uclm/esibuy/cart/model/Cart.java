package es.uclm.esibuy.cart.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("carritos")
public class Cart {

    @Id
    private String id;

    @Field("clienteId")
    private String customerId;

    private List<CartItem> items;

    @Field("precioTotal")
    private BigDecimal totalPrice;

    protected Cart() {
        items = new ArrayList<>();
        totalPrice = BigDecimal.ZERO;
    }

    public Cart(String customerId) {
        this();
        this.customerId = Objects.requireNonNull(customerId);
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void addOrIncrementItem(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity,
            String imageUrl) {

        CartItem existingItem = items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem == null) {
            items.add(new CartItem(productId, name, unitPrice, quantity, imageUrl));
        } else {
            existingItem.addQuantity(quantity);

            if (existingItem.getImageUrl() == null || existingItem.getImageUrl().isBlank()) {
                existingItem.setImageUrl(imageUrl);
            }
        }

        recalculateTotal();
    }

    public void addOrIncrementItem(
            String productId,
            String name,
            BigDecimal unitPrice,
            int quantity) {

        addOrIncrementItem(productId, name, unitPrice, quantity, null);
    }

    public boolean updateItemQuantity(String productId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }

        if (quantity == 0) {
            return removeItem(productId);
        }

        CartItem item = items.stream()
                .filter(existingItem -> existingItem.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (item == null) {
            return false;
        }

        item.setQuantity(quantity);
        recalculateTotal();
        return true;
    }

    public boolean removeItem(String productId) {
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));

        if (removed) {
            recalculateTotal();
        }

        return removed;
    }

    public void clear() {
        items.clear();
        recalculateTotal();
    }

    private void recalculateTotal() {
        totalPrice = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}