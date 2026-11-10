
package es.uclm.esibuy.security.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import es.uclm.esibuy.security.model.EmailOtpChallenge;

public interface EmailOtpChallengeRepository
                extends MongoRepository<EmailOtpChallenge, String> {

        Optional<EmailOtpChallenge> findFirstByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                        String userId,
                        String purpose);

        void deleteByUserIdAndPurpose(
                        String userId,
                        String purpose);

        void deleteAllByUserId(String userId);
}
