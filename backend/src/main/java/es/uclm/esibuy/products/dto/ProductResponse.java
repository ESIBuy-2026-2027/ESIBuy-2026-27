package es.uclm.esibuy.products.dto;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.products.model.Product;

public record ProductResponse(
        String id,
        String name,
        List<String> categoryIds,
        int stock,
        String description,
        BigDecimal price,
        BigDecimal discount,
        BigDecimal premiumDiscount,
        String imageUrl,
        boolean available,
        String sellerId,
        boolean lowStock) {

    public ProductResponse {
        categoryIds = List.copyOf(categoryIds);
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategoryIds(),
                product.getStock(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscount(),
                product.getPremiumDiscount(),
                product.getImageUrl(),
                product.isAvailable(),
                product.getSellerId(),
                product.getStock() > 0 && product.getStock() <= 5);
    }
}
