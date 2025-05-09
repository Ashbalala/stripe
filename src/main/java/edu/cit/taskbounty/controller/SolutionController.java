package edu.cit.taskbounty.controller;

import edu.cit.taskbounty.dto.SubmitSolutionDTO;
import edu.cit.taskbounty.model.Solution;
import edu.cit.taskbounty.service.BountyPostService;
import edu.cit.taskbounty.service.SolutionService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/solutions")
public class SolutionController {

    private static final Logger logger = LoggerFactory.getLogger(SolutionController.class);

    private final SolutionService solutionService;
    private final BountyPostService bountyPostService;

    @Autowired
    public SolutionController(SolutionService solutionService, BountyPostService bountyPostService) {
        this.solutionService = solutionService;
        this.bountyPostService = bountyPostService;
    }

    /**
     * Endpoint to submit a solution.
     * POST /solutions/submit
     */
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitSolution(@RequestBody SubmitSolutionDTO submitSolutionDTO) {
        logger.info("Submitting solution for bountyPostId: {}", submitSolutionDTO.getBountyPostId());
        try {
            ResponseEntity<Solution> response = solutionService.submitSolution(submitSolutionDTO);
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                logger.info("Solution submitted successfully for bountyPostId: {}, solutionId: {}",
                        submitSolutionDTO.getBountyPostId(), response.getBody().getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response.getBody());
            }
            // Handle error cases returned by SolutionService
            String errorMessage;
            HttpStatusCode statusCode = response.getStatusCode();
            if (statusCode.equals(BAD_REQUEST)) {
                errorMessage = "Invalid submission: check bounty post ID, description, or files";
            } else if (statusCode.equals(UNAUTHORIZED)) {
                errorMessage = "User not authenticated";
            } else if (statusCode.equals(NOT_FOUND)) {
                errorMessage = "Bounty post not found";
            } else if (statusCode.equals(FORBIDDEN)) {
                errorMessage = "Cannot submit solution to a non-public bounty post";
            } else {
                errorMessage = "Error submitting solution";
            }
            logger.warn("Failed to submit solution for bountyPostId: {}. Status: {}, Message: {}",
                    submitSolutionDTO.getBountyPostId(), response.getStatusCode(), errorMessage);
            return ResponseEntity.status(response.getStatusCode()).body(errorMessage);
        } catch (Exception e) {
            logger.error("Unexpected error submitting solution for bountyPostId: {}. Error: {}",
                    submitSolutionDTO.getBountyPostId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error submitting solution");
        }
    }

    /**
     * Endpoint to get solutions for a bounty post.
     * Only the bounty post creator can view solutions.
     * GET /solutions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSolutionsByBountyPostId(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching solutions for bountyPostId: {}, page: {}, size: {}", id, page, size);
        try {
            // Validate bounty post and creator
            logger.debug("Validating bounty post: {}", id);
            ResponseEntity<?> bountyPostResponse = bountyPostService.getBountyPostById(new ObjectId(id));
            if (bountyPostResponse.getStatusCode() != HttpStatus.OK) {
                String errorMessage;
                HttpStatusCode statusCode = bountyPostResponse.getStatusCode();
                if (statusCode.equals(NOT_FOUND)) {
                    errorMessage = "Bounty post not found";
                } else if (statusCode.equals(FORBIDDEN)) {
                    errorMessage = "You don't have permission to access this bounty post";
                } else if (statusCode.equals(UNAUTHORIZED)) {
                    errorMessage = "User not authenticated";
                } else {
                    errorMessage = "Error accessing bounty post";
                }
                logger.warn("Failed to fetch solutions for bountyPostId: {}. Status: {}, Message: {}",
                        id, bountyPostResponse.getStatusCode(), errorMessage);
                return ResponseEntity.status(bountyPostResponse.getStatusCode()).body(errorMessage);
            }

            Page<Solution> solutions = solutionService.getSolutionsByBountyPostId(id, page, size);
            logger.info("Retrieved {} solutions for bountyPostId: {}", solutions.getTotalElements(), id);
            return ResponseEntity.ok(solutions);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid bounty post ID: {}. Error: {}", id, e.getMessage());
            return ResponseEntity.status(BAD_REQUEST)
                    .body("Invalid bounty post ID");
        } catch (Exception e) {
            logger.error("Unexpected error fetching solutions for bountyPostId: {}. Error: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error fetching solutions");
        }
    }

    /**
     * Endpoint to delete a solution by its ID.
     * DELETE /solutions/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteSolution(@PathVariable String id) {
        logger.info("Deleting solution with ID: {}", id);
        try {
            ResponseEntity<String> response = solutionService.deleteSolution(id);
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Solution deleted successfully: {}", id);
                return ResponseEntity.ok(response.getBody());
            }
            // Handle error cases from service
            String errorMessage = response.getBody() != null ? response.getBody() : "Error deleting solution";
            HttpStatusCode statusCode = response.getStatusCode();
            logger.warn("Failed to delete solution ID: {}. Status: {}, Message: {}", id, statusCode, errorMessage);
            return ResponseEntity.status(statusCode).body(errorMessage);
        } catch (Exception e) {
            logger.error("Unexpected error deleting solution ID: {}. Error: {}", id, e.getMessage(), e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body("Unexpected error deleting solution");
        }
    }

    /**
     * Endpoint to update a solution by its ID.
     * PATCH /solutions/{id}
     */
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateSolution(@PathVariable String id, @RequestBody SubmitSolutionDTO updateDTO) {
        logger.info("Updating solution with ID: {}", id);
        try {
            ResponseEntity<Solution> response = solutionService.updateSolution(id, updateDTO);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Solution updated successfully: {}", id);
                return ResponseEntity.ok(response.getBody());
            }
            // Handle error cases from service
            String errorMessage;
            HttpStatusCode statusCode = response.getStatusCode();
            if (statusCode.equals(NOT_FOUND)) {
                errorMessage = "Solution not found";
            } else if (statusCode.equals(FORBIDDEN)) {
                errorMessage = "Cannot update another user's solution or an approved solution";
            } else if (statusCode.equals(UNAUTHORIZED)) {
                errorMessage = "User not authenticated";
            } else if (statusCode.equals(BAD_REQUEST)) {
                errorMessage = "Invalid update data";
            } else {
                errorMessage = "Error updating solution";
            }
            logger.warn("Failed to update solution ID: {}. Status: {}, Message: {}", id, statusCode, errorMessage);
            return ResponseEntity.status(statusCode).body(errorMessage);
        } catch (Exception e) {
            logger.error("Unexpected error updating solution ID: {}. Error: {}", id, e.getMessage(), e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body("Unexpected error updating solution");
        }
    }
}