package es.uclm.esibuy.products.model;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
public class Product {

    @Id
    private String id;

    private String name;
    private List<String> categoryIds;
    private int stock;
    private String description;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal premiumDiscount;
    private String imageUrl;
    private boolean available;
    private String sellerId;

    protected Product() {
    }

    public Product(
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
        this.name = name;
        this.categoryIds = List.copyOf(categoryIds);
        this.stock = stock;
        this.description = description;
        this.price = price;
        this.discount = discount;
        this.premiumDiscount = premiumDiscount;
        this.imageUrl = imageUrl;
        this.available = available;
        this.sellerId = sellerId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public int getStock() {
        return stock;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getPremiumDiscount() {
        return premiumDiscount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getSellerId() {
        return sellerId;
    }
}
