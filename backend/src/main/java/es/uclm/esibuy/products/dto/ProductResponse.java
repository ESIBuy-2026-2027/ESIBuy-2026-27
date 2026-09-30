package es.uclm.esibuy.products.dto;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.products.model.Product;

public record ProductResponse(
        String id,
        String nombre,
        List<String> categoriaIds,
        int stock,
        String descripcion,
        BigDecimal precio,
        BigDecimal descuento,
        BigDecimal descuentoPremium,
        String imagen,
        boolean disponible,
        String vendedorId) {

    public ProductResponse {
        categoriaIds = List.copyOf(categoriaIds);
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getNombre(),
                product.getCategoriaIds(),
                product.getStock(),
                product.getDescripcion(),
                product.getPrecio(),
                product.getDescuento(),
                product.getDescuentoPremium(),
                product.getImagen(),
                product.isDisponible(),
                product.getVendedorId());
    }
}
