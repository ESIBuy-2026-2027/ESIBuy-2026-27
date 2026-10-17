package es.uclm.esibuy.cart.model;

import java.math.BigDecimal;
import java.util.Objects;

public class ItemCarrito {

    private String productoId;
    private String nombre;
    private BigDecimal precioUnitario;
    private int cantidad;
    private BigDecimal subtotal;

    protected ItemCarrito() {
    }

    public ItemCarrito(String productoId, String nombre, BigDecimal precioUnitario, int cantidad) {
        this.productoId = Objects.requireNonNull(productoId);
        this.nombre = Objects.requireNonNull(nombre);
        this.precioUnitario = Objects.requireNonNull(precioUnitario);
        setCantidad(cantidad);
    }

    public String getProductoId() {
        return productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    void addCantidad(int cantidad) {
        setCantidad(Math.addExact(this.cantidad, cantidad));
    }

    void setCantidad(int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad debe ser como mínimo 1");
        }
        this.cantidad = cantidad;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
