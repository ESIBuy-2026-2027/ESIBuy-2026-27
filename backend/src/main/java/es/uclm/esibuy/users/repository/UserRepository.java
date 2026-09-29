package es.uclm.esibuy.users.repository;

import java.util.Optional;

import es.uclm.esibuy.users.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsBySellerProfileBusinessNameIgnoreCase(String businessName);

    Optional<User> findByEmailIgnoreCase(String email);
}
