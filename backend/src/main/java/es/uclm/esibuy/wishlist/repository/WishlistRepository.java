package es.uclm.esibuy.wishlist.repository;

import java.util.List;
import java.util.Optional;
import es.uclm.esibuy.wishlist.model.WishlistItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends MongoRepository<WishlistItem, String> {

    List<WishlistItem> findAllByUserIdOrderByAddedAtDesc(String userId);

    Optional<WishlistItem> findByUserIdAndProductId(String userId, String productId);

    boolean existsByUserIdAndProductId(String userId, String productId);

    void deleteByUserIdAndProductId(String userId, String productId);
}

