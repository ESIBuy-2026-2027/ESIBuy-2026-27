package es.uclm.esibuy.cart.dto;

import java.math.BigDecimal;

public record ItemCarritoDTO(
        String productoId,
        String nombre,
        BigDecimal precioUnitario,
        int cantidad,
        BigDecimal subtotal,
        String imageUrl,
        String vendedorId,
        String vendedorNombre,
        BigDecimal precioOriginal,
        BigDecimal descuento,
        Boolean esDescuentoPremium) {

    public ItemCarritoDTO(
            String productoId,
            String nombre,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal) {
        this(productoId, nombre, precioUnitario, cantidad, subtotal, null, null, null, precioUnitario, BigDecimal.ZERO, false);
    }

    public ItemCarritoDTO(
            String productoId,
            String nombre,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal,
            String imageUrl) {
        this(productoId, nombre, precioUnitario, cantidad, subtotal, imageUrl, null, null, precioUnitario, BigDecimal.ZERO, false);
    }

    public ItemCarritoDTO(
            String productoId,
            String nombre,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal,
            String imageUrl,
            String vendedorId) {
        this(productoId, nombre, precioUnitario, cantidad, subtotal, imageUrl, vendedorId, null, precioUnitario, BigDecimal.ZERO, false);
    }

    public ItemCarritoDTO(
            String productoId,
            String nombre,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal,
            String imageUrl,
            String vendedorId,
            String vendedorNombre) {
        this(productoId, nombre, precioUnitario, cantidad, subtotal, imageUrl, vendedorId, vendedorNombre, precioUnitario, BigDecimal.ZERO, false);
    }

    public ItemCarritoDTO(
            String productoId,
            String nombre,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal,
            String imageUrl,
            BigDecimal precioOriginal,
            BigDecimal descuento,
            Boolean esDescuentoPremium) {
        this(productoId, nombre, precioUnitario, cantidad, subtotal, imageUrl, null, null, precioOriginal, descuento, esDescuentoPremium);
    }
}
