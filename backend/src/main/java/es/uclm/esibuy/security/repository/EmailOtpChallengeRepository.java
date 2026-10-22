
package es.uclm.esibuy.security.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import es.uclm.esibuy.security.model.EmailOtpChallenge;

public interface EmailOtpChallengeRepository
        extends MongoRepository<EmailOtpChallenge, String> {

    Optional<EmailOtpChallenge> findFirstByUserIdAndUsedFalseOrderByExpiresAtDesc(
            String userId);

    void deleteByUserId(String userId);
}
