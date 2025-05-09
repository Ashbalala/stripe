package edu.cit.taskbounty.repository;

import edu.cit.taskbounty.model.Solution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SolutionRepository extends MongoRepository<Solution, String> {

    // Find the approved solution for a bounty post
    Solution findByBountyPostIdAndApprovedTrue(String bountyPostId);

    // Find solutions by bounty post ID with pagination
    Page<Solution> findByBountyPostId(String bountyPostId, Pageable pageable);

    // New method to find a solution by its ID and bounty post ID
    Optional<Solution> findByIdAndBountyPostId(String solutionId, String bountyPostId);
}