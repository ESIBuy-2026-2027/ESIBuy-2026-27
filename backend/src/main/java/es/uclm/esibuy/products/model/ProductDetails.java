
package es.uclm.esibuy.products.model;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetails(
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
}
