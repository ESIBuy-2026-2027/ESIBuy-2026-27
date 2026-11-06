package es.uclm.esibuy.reviews.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("reviews")
@CompoundIndex(
        name = "customer_order_product_unique",
        def = "{'customerId': 1, 'orderId': 1, 'productId': 1}",
        unique = true)
public class Review {

    @Id
    private String id;
    private String customerId;
    private String orderId;
    private String productId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public Review() {
    }

    public Review(
            String customerId,
            String orderId,
            String productId,
            int rating,
            String comment) {

        this.customerId = customerId;
        this.orderId = orderId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
