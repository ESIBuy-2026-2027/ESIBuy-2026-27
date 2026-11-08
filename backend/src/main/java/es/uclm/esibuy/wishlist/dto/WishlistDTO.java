package es.uclm.esibuy.wishlist.dto;

import java.time.Instant;
import java.util.List;
import es.uclm.esibuy.wishlist.model.Wishlist;

public record WishlistDTO(
        String id,
        String name,
        int itemCount,
        List<String> productIds,
        Instant createdAt
) {
    public static WishlistDTO from(Wishlist wishlist) {
        return new WishlistDTO(
                wishlist.getId(),
                wishlist.getName(),
                wishlist.getProductIds() != null ? wishlist.getProductIds().size() : 0,
                wishlist.getProductIds() != null ? List.copyOf(wishlist.getProductIds()) : List.of(),
                wishlist.getCreatedAt()
        );
    }
}
