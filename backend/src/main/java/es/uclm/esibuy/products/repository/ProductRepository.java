package es.uclm.esibuy.products.repository;

import es.uclm.esibuy.products.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

    long countByVendedorId(String vendedorId);

    long countByVendedorIdAndStock(String vendedorId, int stock);

    long countByVendedorIdAndStockGreaterThanAndStockLessThanEqual(
            String vendedorId,
            int minimoExclusivo,
            int maximoInclusivo);
}
