package es.uclm.esibuy.products.repository;

import es.uclm.esibuy.products.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    boolean existsByCategoryIdsContaining(String categoryId);

    List<Product> findAllBySellerId(String sellerId);

    long countBySellerId(String sellerId);

    long countBySellerIdAndStock(String sellerId, int stock);

    long countBySellerIdAndStockGreaterThanAndStockLessThanEqual(
            String sellerId,
            int minimumExclusive,
            int maximumInclusive);
}
