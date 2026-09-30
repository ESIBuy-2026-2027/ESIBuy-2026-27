package es.uclm.esibuy.products.dto;

public record ProductSummaryResponse(
        long totalProducts,
        long outOfStockProducts,
        long lowStockProducts,
        int lowStockThreshold) {
}
