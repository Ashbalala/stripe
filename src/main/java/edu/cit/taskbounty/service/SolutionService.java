package edu.cit.taskbounty.service;

import edu.cit.taskbounty.dto.SubmitSolutionDTO;
import edu.cit.taskbounty.model.BountyPost;
import edu.cit.taskbounty.model.Solution;
import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.BountyPostRepository;
import edu.cit.taskbounty.repository.SolutionRepository;
import edu.cit.taskbounty.repository.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SolutionService {

    private static final Logger logger = LoggerFactory.getLogger(SolutionService.class);

    @Autowired
    private BountyPostService bountyPostService;

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private BountyPostRepository bountyPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private static final List<String> ALLOWED_FILE_TYPES = List.of(
            "image/jpeg", "image/png", "application/pdf", "text/plain",
            "application/zip"
    );

    private static final int MAX_FILES = 5;

    public SolutionService(SolutionRepository solutionRepository,
                           BountyPostRepository bountyPostRepository,
                           UserRepository userRepository) {
        this.solutionRepository = solutionRepository;
        this.bountyPostRepository = bountyPostRepository;
        this.userRepository = userRepository;
    }

    public Page<Solution> getSolutionsByBountyPostId(String bountyPostId, int page, int size) {
        logger.debug("Fetching solutions for bountyPostId: {}, page: {}, size: {}", bountyPostId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Solution> solutions = solutionRepository.findByBountyPostId(bountyPostId, pageable);
        logger.info("Retrieved {} solutions for bountyPostId: {}", solutions.getTotalElements(), bountyPostId);
        return solutions;
    }

    public Optional<Solution> getSolutionByIdAndBountyPostId(String solutionId, String bountyPostId) {
        logger.debug("Fetching solutionId: {} for bountyPostId: {}", solutionId, bountyPostId);
        Optional<Solution> solution = solutionRepository.findByIdAndBountyPostId(solutionId, bountyPostId);
        if (solution.isPresent()) {
            logger.info("Solution found: {} for bountyPostId: {}", solutionId, bountyPostId);
        } else {
            logger.warn("Solution not found: {} for bountyPostId: {}", solutionId, bountyPostId);
        }
        return solution;
    }

    /**
     * Submit a new solution.
     */
    @Transactional
    public ResponseEntity<Solution> submitSolution(SubmitSolutionDTO submittedSolution) {
        logger.info("Submitting solution for bountyPostId: {}", submittedSolution.getBountyPostId());

        // Validate inputs
        String bountyPostId = submittedSolution.getBountyPostId();
        if (bountyPostId == null || bountyPostId.trim().isEmpty()) {
            logger.error("Bounty post ID is null or empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
        if (submittedSolution.getDescription() == null || submittedSolution.getDescription().trim().isEmpty()) {
            logger.error("Solution description is null or empty for bountyPostId: {}", bountyPostId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

        // Authenticate user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        logger.debug("Fetching user: {}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        // Validate bounty post
        logger.debug("Validating bounty post: {}", bountyPostId);
        ResponseEntity<BountyPost> bountyPostResponse;
        try {
            bountyPostResponse = bountyPostService.getBountyPostById(new ObjectId(bountyPostId));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid bounty post ID: {}", bountyPostId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

        if (bountyPostResponse.getStatusCode() != HttpStatus.OK || bountyPostResponse.getBody() == null) {
            logger.warn("Bounty post {} not found or inaccessible", bountyPostId);
            return ResponseEntity.status(bountyPostResponse.getStatusCode() == HttpStatus.OK ? HttpStatus.NOT_FOUND : bountyPostResponse.getStatusCode())
                    .body(null);
        }

        BountyPost bountyPost = bountyPostResponse.getBody();
        if (!bountyPost.isPublic()) {
            logger.error("Bounty post {} is not public", bountyPostId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

        // Check for existing approved solution
        logger.debug("Checking for existing approved solution for bountyPostId: {}", bountyPostId);
        Solution approvedSolution = solutionRepository.findByBountyPostIdAndApprovedTrue(bountyPostId);
        if (approvedSolution != null) {
            logger.warn("Approved solution already exists for bountyPostId: {}", bountyPostId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

        // Create upload directory
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            logger.debug("Upload directory ensured: {}", uploadPath);
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", uploadPath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }

        // Build and save solution
        try {
            Solution solution = new Solution();
            solution.setBountyPostId(bountyPostId);
            solution.setSubmitterId(user.getId());
            solution.setContent(submittedSolution.getDescription().trim());
            solution.setApproved(false);
            solution.setCreatedAt(LocalDateTime.now());

            logger.debug("Saving solution for bountyPostId: {}, submitterId: {}", bountyPostId, user.getId());
            Solution savedSolution = solutionRepository.save(solution);
            logger.info("Solution saved successfully for bountyPostId: {}, solutionId: {}",
                    bountyPostId, savedSolution.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSolution);
        } catch (Exception e) {
            logger.error("Failed to save solution for bountyPostId: {}", bountyPostId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @Transactional
    public ResponseEntity<String> deleteSolution(String solutionId) {
        logger.info("Deleting solution with ID: {}", solutionId);

        // Authenticate user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        logger.debug("Fetching user: {}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not authenticated");
        }

        // Find the solution
        Optional<Solution> solutionOpt = solutionRepository.findById(solutionId);
        if (solutionOpt.isEmpty()) {
            logger.warn("Solution not found: {}", solutionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Solution not found");
        }
        Solution solution = solutionOpt.get();

        // Check ownership
        if (!solution.getSubmitterId().equals(user.getId())) {
            logger.warn("User {} attempted to delete solution {} owned by another user", user.getId(), solutionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Cannot delete another user's solution");
        }

        // Check if the solution is approved
        if (solution.isApproved()) {
            logger.warn("Attempted to delete approved solution: {}", solutionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Cannot delete an approved solution");
        }

        // Delete the solution
        try {
            solutionRepository.deleteById(solutionId);
            logger.info("Solution deleted successfully: {}", solutionId);
            return ResponseEntity.ok("Solution deleted successfully");
        } catch (Exception e) {
            logger.error("Failed to delete solution: {}", solutionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete solution");
        }
    }

    /**
     * Update a solution by its ID.
     */
    @Transactional
    public ResponseEntity<Solution> updateSolution(String solutionId, SubmitSolutionDTO updateDTO) {
        logger.info("Updating solution with ID: {}", solutionId);

        // Authenticate user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        logger.debug("Fetching user: {}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        // Find the solution
        Optional<Solution> solutionOpt = solutionRepository.findById(solutionId);
        if (solutionOpt.isEmpty()) {
            logger.warn("Solution not found: {}", solutionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
        Solution solution = solutionOpt.get();

        // Check ownership
        if (!solution.getSubmitterId().equals(user.getId())) {
            logger.warn("User {} attempted to update solution {} owned by another user", user.getId(), solutionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        // Update fields if provided
        if (updateDTO.getDescription() != null && !updateDTO.getDescription().trim().isEmpty()) {
            solution.setContent(updateDTO.getDescription().trim());
        }

        // Save updated solution
        try {
            Solution updatedSolution = solutionRepository.save(solution);
            logger.info("Solution updated successfully: {}", solutionId);
            return ResponseEntity.ok(updatedSolution);
        } catch (Exception e) {
            logger.error("Failed to update solution: {}", solutionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }



}