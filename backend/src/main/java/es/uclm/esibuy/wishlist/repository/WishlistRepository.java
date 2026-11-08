package es.uclm.esibuy.wishlist.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import es.uclm.esibuy.wishlist.model.Wishlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {

    List<Wishlist> findAllByUserIdOrderByCreatedAtAsc(String userId);

    Optional<Wishlist> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndNameIgnoreCase(String userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(String userId, String name, String id);

    void deleteAllByUserId(String userId);

    List<Wishlist> findAllByProductIdsContaining(String productId);

    List<Wishlist> findAllByProductIdsIn(Collection<String> productIds);

    default void deleteAllByProductIdIn(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Wishlist> affected = findAllByProductIdsIn(productIds);
        for (Wishlist wishlist : affected) {
            wishlist.getProductIds().removeAll(productIds);
        }
        saveAll(affected);
    }
}
