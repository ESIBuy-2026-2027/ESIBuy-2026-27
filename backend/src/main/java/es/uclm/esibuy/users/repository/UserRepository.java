package es.uclm.esibuy.users.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsBySellerProfileBusinessNameIgnoreCase(String businessName);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRoleIn(Collection<UserRole> roles);
}
