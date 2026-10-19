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
    private Double rating = 5.0;

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

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds != null ? List.copyOf(categoryIds) : List.of();
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public void setPremiumDiscount(BigDecimal premiumDiscount) {
        this.premiumDiscount = premiumDiscount;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public Double getRating() {
        return rating != null ? rating : 5.0;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public void updateDetails(
            String name,
            List<String> categoryIds,
            int stock,
            String description,
            BigDecimal price,
            BigDecimal discount,
            BigDecimal premiumDiscount,
            String imageUrl,
            boolean available) {
        this.name = name;
        this.categoryIds = categoryIds != null ? List.copyOf(categoryIds) : List.of();
        this.stock = stock;
        this.description = description;
        this.price = price;
        this.discount = discount;
        this.premiumDiscount = premiumDiscount;
        this.imageUrl = imageUrl;
        this.available = available;
    }
}
