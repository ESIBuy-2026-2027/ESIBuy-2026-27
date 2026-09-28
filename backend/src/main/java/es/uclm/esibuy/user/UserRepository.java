package es.uclm.esibuy.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByBusinessNameIgnoreCase(String businessName);

    Optional<User> findByEmailIgnoreCase(String email);
}
