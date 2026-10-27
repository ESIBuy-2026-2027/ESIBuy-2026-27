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
        Double rating,
        String sellerName) {

    public ProductResponse {
        categoryIds = List.copyOf(categoryIds);
    }

    public ProductResponse(
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
            String sellerId) {
        this(id, name, categoryIds, stock, description, price, discount, premiumDiscount, imageUrl, available, sellerId, 5.0, null);
    }

    public ProductResponse(
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
            Double rating) {
        this(id, name, categoryIds, stock, description, price, discount, premiumDiscount, imageUrl, available, sellerId, rating, null);
    }

    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, String sellerName) {
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
                product.getRating(),
                sellerName);
    }
}
