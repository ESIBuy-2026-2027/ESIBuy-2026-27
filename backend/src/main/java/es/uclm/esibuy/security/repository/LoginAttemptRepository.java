package es.uclm.esibuy.security.repository;

import java.util.Optional;

import es.uclm.esibuy.security.model.LoginAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LoginAttemptRepository extends MongoRepository<LoginAttempt, String> {

    Optional<LoginAttempt> findByEmailAndIpAddress(String email, String ipAddress);

    void deleteByEmailAndIpAddress(String email, String ipAddress);

    void deleteAllByEmail(String email);
}