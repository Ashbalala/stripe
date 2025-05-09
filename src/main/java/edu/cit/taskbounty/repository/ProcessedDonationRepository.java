package edu.cit.taskbounty.repository;

import edu.cit.taskbounty.model.ProcessedDonation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedDonationRepository extends MongoRepository<ProcessedDonation, String> {
    boolean existsBySessionId(String sessionId);
}
