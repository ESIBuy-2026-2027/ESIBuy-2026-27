package es.uclm.esibuy.products.repository;

import es.uclm.esibuy.products.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
