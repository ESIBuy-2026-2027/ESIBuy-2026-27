package es.uclm.esibuy.cart.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("carritos")
public class Carrito {

    @Id
    private String id;
    private String clienteId;
    private List<ItemCarrito> items;
    private BigDecimal precioTotal;

    protected Carrito() {
        items = new ArrayList<>();
        precioTotal = BigDecimal.ZERO;
    }

    public Carrito(String clienteId) {
        this();
        this.clienteId = Objects.requireNonNull(clienteId);
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public List<ItemCarrito> getItems() {
        return List.copyOf(items);
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public void addOrIncrementItem(
            String productoId,
            String nombre,
            BigDecimal precioUnitario,
            int cantidad,
            String imageUrl) {
        ItemCarrito existingItem = items.stream()
                .filter(item -> item.getProductoId().equals(productoId))
                .findFirst()
                .orElse(null);
        if (existingItem == null) {
            items.add(new ItemCarrito(productoId, nombre, precioUnitario, cantidad, imageUrl));
        } else {
            existingItem.addCantidad(cantidad);
            if (existingItem.getImageUrl() == null || existingItem.getImageUrl().isBlank()) {
                existingItem.setImageUrl(imageUrl);
            }
        }
        recalculateTotal();
    }

    public void addOrIncrementItem(String productoId, String nombre, BigDecimal precioUnitario, int cantidad) {
        addOrIncrementItem(productoId, nombre, precioUnitario, cantidad, null);
    }

    public boolean updateItemQuantity(String productoId, int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        if (cantidad == 0) {
            return removeItem(productoId);
        }

        ItemCarrito item = items.stream()
                .filter(existingItem -> existingItem.getProductoId().equals(productoId))
                .findFirst()
                .orElse(null);
        if (item == null) {
            return false;
        }

        item.setCantidad(cantidad);
        recalculateTotal();
        return true;
    }

    public boolean removeItem(String productoId) {
        boolean removed = items.removeIf(item -> item.getProductoId().equals(productoId));
        if (removed) {
            recalculateTotal();
        }
        return removed;
    }

    public void vaciar() {
        items.clear();
        recalculateTotal();
    }

    private void recalculateTotal() {
        precioTotal = items.stream()
                .map(ItemCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
