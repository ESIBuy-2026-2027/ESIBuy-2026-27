package es.uclm.esibuy.products.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import es.uclm.esibuy.products.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {

    boolean existsByCategoryIdsContaining(String categoryId);

    List<Product> findAllBySellerId(String sellerId);

    List<Product> findAllByAvailableTrue();

    List<Product> findAllByAvailableTrueAndStockGreaterThan(int stock);

    long countBySellerId(String sellerId);

    long countBySellerIdAndStock(String sellerId, int stock);

    long countBySellerIdAndStockGreaterThanAndStockLessThanEqual(
            String sellerId,
            int minimumExclusive,
            int maximumInclusive);
}
