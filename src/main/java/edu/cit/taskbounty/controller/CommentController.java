package edu.cit.taskbounty.controller;

import edu.cit.taskbounty.model.Comment;
import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.BountyPostRepository;
import edu.cit.taskbounty.repository.UserRepository;
import edu.cit.taskbounty.service.CommentService;
import edu.cit.taskbounty.dto.CommentRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BountyPostRepository bountyPostRepository;

    /**
     * Create a new comment or reply.
     * Gets the authenticated user from security context instead of request body.
     */
    @PostMapping("/{postId}/bounty_post")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> createComment(
            @PathVariable("postId") String postId,
            @RequestBody CommentRequest commentRequest) {
        // Validate postId
        if (!ObjectId.isValid(postId)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String authorId = user.getId();
        Comment comment = commentService.createComment(
                postId,
                commentRequest.getParentCommentId(),
                authorId,
                commentRequest.getContent()
        );
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }

    /**
     * Get all comments for a BountyPost.
     */
    @GetMapping("/{postId}/bounty_post")
    public ResponseEntity<List<Comment>> getCommentsByBountyPostId(
            @PathVariable("postId") String postId) {
        List<Comment> comments = commentService.getCommentsByBountyPostId(postId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }

    /**
     * Get a specific comment by ID.
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<Comment> getCommentById(
            @PathVariable("commentId") String commentId) {
        Optional<Comment> comment = commentService.getCommentById(commentId);
        return comment.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Update a comment.
     * Ensures only the author can update their comment.
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> updateComment(
            @PathVariable("commentId") String commentId,
            @RequestBody CommentRequest commentRequest) {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String userId = user.getId();

        // Fetch the existing comment
        Optional<Comment> optionalComment = commentService.getCommentById(commentId);
        if (!optionalComment.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Comment existingComment = optionalComment.get();

        // Check if the user is the author
        if (!existingComment.getAuthorId().equals(userId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        // Prepare comment object for update
        Comment commentToUpdate = new Comment();
        commentToUpdate.setId(commentId);
        commentToUpdate.setContent(commentRequest.getContent());

        Comment updatedComment = commentService.updateComment(commentToUpdate);
        return new ResponseEntity<>(updatedComment, HttpStatus.OK);
    }

    /**
     * Delete a comment and its replies.
     * Ensures only the author can delete their comment.
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("commentId") String commentId) {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String userId = user.getId();

        // Fetch the comment
        Optional<Comment> optionalComment = commentService.getCommentById(commentId);
        if (!optionalComment.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Comment comment = optionalComment.get();

        // Check if the user is the author
        if (!comment.getAuthorId().equals(userId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        commentService.deleteComment(commentId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

