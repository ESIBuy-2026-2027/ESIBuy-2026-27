package es.uclm.esibuy.reviews.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import es.uclm.esibuy.reviews.model.Review;

public interface ReviewRepository extends MongoRepository<Review, String> {

    boolean existsByCustomerIdAndOrderIdAndProductId(
            String customerId,
            String orderId,
            String productId);
}
