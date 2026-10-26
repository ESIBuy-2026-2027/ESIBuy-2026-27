package es.uclm.esibuy.products.dto;

import java.math.BigDecimal;

import es.uclm.esibuy.products.model.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductDetailDTO(
        @NotBlank(message = "Product ID cannot be blank")
        String id,

        @NotBlank(message = "Product name cannot be blank")
        @Size(max = 150, message = "Product name cannot exceed 150 characters")
        String name,

        @Size(max = 2000, message = "Product description cannot exceed 2000 characters")
        String description,

        @NotNull(message = "Product price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Product price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Product stock is required")
        @PositiveOrZero(message = "Product stock cannot be negative")
        Integer stock,

        String category,

        @NotBlank(message = "Seller ID cannot be blank")
        String sellerId,

        String sellerName,

        @NotNull(message = "Product active status is required")
        Boolean active) {

    public static ProductDetailDTO from(Product product, String categoryName, String sellerName) {
        return new ProductDetailDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                categoryName,
                product.getSellerId(),
                sellerName,
                product.isAvailable());
    }

    public static ProductDetailDTO from(Product product) {
        String firstCategory = (product.getCategoryIds() != null && !product.getCategoryIds().isEmpty())
                ? product.getCategoryIds().getFirst()
                : null;
        return from(product, firstCategory, null);
    }
}

