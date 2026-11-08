package es.uclm.esibuy.wishlist.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("wishlists")
@CompoundIndex(name = "user_name_idx", def = "{'userId': 1, 'name': 1}", unique = false)
public class Wishlist {

    @Id
    private String id;

    private String userId;
    private String name;
    private List<String> productIds = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public Wishlist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.productIds = new ArrayList<>();
    }

    public Wishlist(String userId, String name) {
        this.userId = userId;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.productIds = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public List<String> getProductIds() {
        if (productIds == null) {
            productIds = new ArrayList<>();
        }
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds != null ? productIds : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
