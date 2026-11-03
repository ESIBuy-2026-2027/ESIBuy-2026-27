package es.uclm.esibuy.cart.repository;

import java.util.Optional;

import es.uclm.esibuy.cart.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByCustomerId(String customerId);

    void deleteByCustomerId(String customerId);
}