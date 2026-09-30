package es.uclm.esibuy.products.dto;

public record ProductSummaryResponse(
        long totalProductos,
        long productosSinStock,
        long productosStockBajo,
        int umbralStockBajo) {
}
