package edu.cit.taskbounty.service;

import edu.cit.taskbounty.dto.BountyPostRequest;
import edu.cit.taskbounty.model.BountyPost;
import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.BountyPostRepository;
import edu.cit.taskbounty.repository.UserRepository;
import edu.cit.taskbounty.util.JwtUtil;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BountyPostService {

    private static final Logger logger = LoggerFactory.getLogger(BountyPostService.class);

    private final BountyPostRepository bountyPostRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;

    public BountyPostService(BountyPostRepository bountyPostRepository) {
        this.bountyPostRepository = bountyPostRepository;
    }

    public Page<BountyPost> getBountyPosts(int page, int size, String sortBy, String search) {
        size = Math.min(size, 25);
        Sort sort;

        // Determine sort order based on sortBy parameter
        sort = switch (sortBy.toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "upvotes");
        };

        Pageable pageable = PageRequest.of(page, size, sort);

        // Only show public posts (not filtering by creator anymore)
        if (search != null && !search.trim().isEmpty()) {
            // Use only public posts in search
            return bountyPostRepository.searchPublicBountyPosts(search, pageable);
        }

        // Return only public posts
        return bountyPostRepository.findAllPublic(pageable);
    }

    public Page<BountyPost> getDraftBountyPosts(int page, int size) {
        if (!isUserAuthenticated()) {
            throw new AuthenticationRequiredException("Authentication required to access draft posts");
        }

        User user = getCurrentUser();

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        return bountyPostRepository.findDraftsByCreatorId(user.getId(), pageable);
    }

    public ResponseEntity<BountyPost> getDraftBountyPost(ObjectId id) {
        if (!isUserAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        User user = getCurrentUser();

        Optional<BountyPost> bountyPost = bountyPostRepository.findDraftByIdAndCreatorId(id, user.getId());
        if (bountyPost.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        return ResponseEntity.ok(bountyPost.get());
    }

    public ResponseEntity<BountyPost> getBountyPostById(ObjectId id) {
        Optional<BountyPost> post = bountyPostRepository.findById(id);

        if (post.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        // If post is public, return it
        if (post.get().isPublic()) {
            return ResponseEntity.ok(post.get());
        }

        // If post is not public, check if the current user is the creator
        if (!isUserAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(post.get().getCreatorId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        return ResponseEntity.ok(post.get());
    }

    public boolean vote(String bountyPostId, String voteType) {
        if (!isUserAuthenticated()) {
            throw new AuthenticationRequiredException("Authentication required to vote");
        }
        User user = getCurrentUser();
        BountyPost post = bountyPostRepository.findById(new ObjectId(bountyPostId))
                .orElseThrow(() -> new RuntimeException("Bounty post not found"));

        boolean isUpvote = "upvote".equalsIgnoreCase(voteType);
        boolean isDownvote = "downvote".equalsIgnoreCase(voteType);

        if (!isUpvote && !isDownvote) {
            throw new IllegalArgumentException("Invalid vote type: " + voteType);
        }

        if (isUpvote) {
            if (post.getVotedUp().contains(user.getId())) {
                logger.info("User {} already upvoted post {}", user.getUsername(), bountyPostId);
                return false;
            }
            if (post.getVotedDown().contains(user.getId())) {
                post.getVotedDown().remove(user.getId());
                post.setDownvotes(post.getDownvotes() - 1);
            }
            post.getVotedUp().add(user.getId());
            post.setUpvotes(post.getUpvotes() + 1);
            logger.info("User {} upvoted post {}", user.getId(), bountyPostId);
        } else {
            if (post.getVotedDown().contains(user.getId())) {
                logger.info("User {} already downvoted post {}", user.getId(), bountyPostId);
                return false;
            }
            if (post.getVotedUp().contains(user.getId())) {
                post.getVotedUp().remove(user.getId());
                post.setUpvotes(post.getUpvotes() - 1);
            }
            post.getVotedDown().add(String.valueOf(user.getId()));
            post.setDownvotes(post.getDownvotes() + 1);
            logger.info("User {} downvoted post {}", user.getId(), bountyPostId);
        }

        bountyPostRepository.save(post);
        return true;
    }

    private boolean isUserAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    public ResponseEntity<BountyPost> createBountyPost(BountyPostRequest bountyPostRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username);

        BountyPost bountyPost = new BountyPost();
        bountyPost.setTitle(bountyPostRequest.getTitle());
        bountyPost.setDescription(bountyPostRequest.getDescription());
        bountyPost.setBountyPrice(bountyPostRequest.getBountyPrice());
        bountyPost.setCreatorId(user.getId());
        bountyPost.setPublic(false);

        BountyPost savedPost = bountyPostRepository.save(bountyPost);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPost);
    }

    public ResponseEntity<Void> deleteBountyPost(ObjectId id) {
        if (!isUserAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        User user = getCurrentUser();

        Optional<BountyPost> post = bountyPostRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .build();
        }

        // Check if the user owns the post
        if (!post.get().getCreatorId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        }

        // Delete the post
        bountyPostRepository.delete(post.get());
        logger.info("User {} deleted bounty post {}", user.getId(), id);
        return ResponseEntity.ok().build();
    }
}