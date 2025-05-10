package edu.cit.taskbounty.controller;

import edu.cit.taskbounty.model.Comment;
import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.BountyPostRepository;
import edu.cit.taskbounty.repository.UserRepository;
import edu.cit.taskbounty.service.CommentService;
import edu.cit.taskbounty.dto.CommentRequest;
import edu.cit.taskbounty.dto.CommentResponse;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BountyPostRepository bountyPostRepository;

    @PostMapping("/{postId}/bounty_post")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> createComment(
            @PathVariable("postId") String postId,
            @RequestBody CommentRequest commentRequest) {
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

    @GetMapping("/{postId}/bounty_post")
    public ResponseEntity<List<CommentResponse>> getCommentsByBountyPostId(
            @PathVariable("postId") String postId) {
        List<Comment> comments = commentService.getCommentsByBountyPostId(postId);
        List<CommentResponse> responseList = comments.stream().map(comment -> {
            User user = userRepository.findById(comment.getAuthorId()).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";
            return new CommentResponse(
                    comment.getId(),
                    comment.getBountyPostId(),
                    comment.getParentCommentId(),
                    comment.getAuthorId(),
                    username,
                    comment.getContent(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt()
            );
        }).collect(Collectors.toList());

        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<Comment> getCommentById(
            @PathVariable("commentId") String commentId) {
        Optional<Comment> comment = commentService.getCommentById(commentId);
        return comment.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> updateComment(
            @PathVariable("commentId") String commentId,
            @RequestBody CommentRequest commentRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String userId = user.getId();

        Optional<Comment> optionalComment = commentService.getCommentById(commentId);
        if (!optionalComment.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Comment existingComment = optionalComment.get();

        if (!existingComment.getAuthorId().equals(userId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Comment commentToUpdate = new Comment();
        commentToUpdate.setId(commentId);
        commentToUpdate.setContent(commentRequest.getContent());

        Comment updatedComment = commentService.updateComment(commentToUpdate);
        return new ResponseEntity<>(updatedComment, HttpStatus.OK);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("commentId") String commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String userId = user.getId();

        Optional<Comment> optionalComment = commentService.getCommentById(commentId);
        if (!optionalComment.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Comment comment = optionalComment.get();

        if (!comment.getAuthorId().equals(userId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        commentService.deleteComment(commentId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}