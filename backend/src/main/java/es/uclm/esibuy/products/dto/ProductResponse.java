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
        long price,
        float discount,
        float premiumDiscount,
        String imageUrl,
        boolean available,
        String sellerId) {

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
                product.getSellerId());
    }
}
