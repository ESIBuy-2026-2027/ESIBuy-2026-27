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
    private String imageFileId;
    private String imageContentType;
    private boolean available;
    private String sellerId;
    private Double rating = 5.0;

    protected Product() {
    }

    public Product(ProductDetails details) {
        this.name = details.name();
        this.categoryIds = details.categoryIds() != null
                ? List.copyOf(details.categoryIds())
                : List.of();
        this.stock = details.stock();
        this.description = details.description();
        this.price = details.price();
        this.discount = details.discount();
        this.premiumDiscount = details.premiumDiscount();
        this.imageUrl = details.imageUrl();
        this.available = details.available();
        this.sellerId = details.sellerId();
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

    public String getImageFileId() {
        return imageFileId;
    }

    public void setImageFileId(String imageFileId) {
        this.imageFileId = imageFileId;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
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

    public void updateDetails(ProductDetails details) {
        this.name = details.name();
        this.categoryIds = details.categoryIds() != null
                ? List.copyOf(details.categoryIds())
                : List.of();
        this.stock = details.stock();
        this.description = details.description();
        this.price = details.price();
        this.discount = details.discount();
        this.premiumDiscount = details.premiumDiscount();
        this.imageUrl = details.imageUrl();
        this.available = details.available();
    }
}
