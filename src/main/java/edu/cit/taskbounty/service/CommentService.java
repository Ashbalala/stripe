package edu.cit.taskbounty.service;

import edu.cit.taskbounty.model.BountyPost;
import edu.cit.taskbounty.model.Comment;
import edu.cit.taskbounty.repository.BountyPostRepository;
import edu.cit.taskbounty.repository.CommentRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BountyPostRepository bountyPostRepository;

    /**
     * Create a new comment or reply (authenticated users only).
     */
    public Comment createComment(String bountyPostId, String parentCommentId, String authorId, String content) {
        BountyPost post = bountyPostRepository.findById(new ObjectId(bountyPostId))
                .orElseThrow(() -> new RuntimeException("BountyPost not found"));
        if (!post.isPublic()) {
            throw new RuntimeException("Cannot comment on a non-public BountyPost");
        }
        Comment comment = new Comment();
        comment.setParentCommentId(parentCommentId);
        comment.setBountyPostId(bountyPostId);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment = commentRepository.save(comment);
        bountyPostRepository.save(post);
        return comment;
    }

    /**
     * Get all comments for a BountyPost (public posts only).
     */
    public List<Comment> getCommentsByBountyPostId(String bountyPostId) {
        BountyPost post = bountyPostRepository.findById(new ObjectId(bountyPostId))
                .orElseThrow(() -> new RuntimeException("BountyPost not found"));

        if (!post.isPublic()) {
            throw new RuntimeException("BountyPost is not public");
        }

        return commentRepository.findByBountyPostId(new ObjectId(bountyPostId));
    }

    /**
     * Get a comment by ID (public posts only).
     */
    public Optional<Comment> getCommentById(String id) {
        Optional<Comment> comment = commentRepository.findById(new ObjectId(id));
        if (comment.isPresent()) {
            BountyPost post = bountyPostRepository.findById(new ObjectId(comment.get().getBountyPostId()))
                    .orElseThrow(() -> new RuntimeException("BountyPost not found"));
            if (post.isPublic()) {
                return comment;
            }
        }
        return Optional.empty();
    }

    /**
     * Update a comment (author only).
     */
    @PreAuthorize("isAuthenticated() and #comment.authorId == principal.id")
    public Comment updateComment(Comment comment) {
        Comment existingComment = commentRepository.findById(new ObjectId(comment.getId()))
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        existingComment.setContent(comment.getContent());
        return commentRepository.save(existingComment);
    }

    /**
     * Delete a comment and its replies (author only).
     */
    @PreAuthorize("isAuthenticated() and #id == principal.id")
    public void deleteComment(String id) {
        Comment comment = commentRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Recursively delete comment and its replies
        deleteCommentAndReplies(comment);

        // Update comment count in BountyPost
        BountyPost post = bountyPostRepository.findById(new ObjectId(comment.getBountyPostId()))
                .orElseThrow(() -> new RuntimeException("BountyPost not found"));
        bountyPostRepository.save(post);
    }

    private void deleteCommentAndReplies(Comment comment) {
        List<Comment> replies = commentRepository.findByParentCommentId(new ObjectId(comment.getId()));
        for (Comment reply : replies) {
            deleteCommentAndReplies(reply);
        }
        commentRepository.delete(comment);
    }

}