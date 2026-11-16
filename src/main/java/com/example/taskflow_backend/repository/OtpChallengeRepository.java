package com.example.taskflow_backend.repository;

import com.example.taskflow_backend.enums.OtpPurpose;
import com.example.taskflow_backend.model.OtpChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpChallengeRepository extends MongoRepository<OtpChallenge, String> {
    Optional<OtpChallenge> findByUserIdAndPurpose(String userId, OtpPurpose purpose);
    void deleteByUserIdAndPurpose(String userId, OtpPurpose purpose);
}
