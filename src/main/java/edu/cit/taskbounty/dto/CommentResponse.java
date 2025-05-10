package edu.cit.taskbounty.dto;

import java.time.Instant;

public class CommentResponse {
    private String id;
    private String bountyPostId;
    private String parentCommentId;
    private String authorId;
    private String authorUsername;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public CommentResponse() {
    }

    public CommentResponse(
            String id,
            String bountyPostId,
            String parentCommentId,
            String authorId,
            String authorUsername,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.bountyPostId = bountyPostId;
        this.parentCommentId = parentCommentId;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBountyPostId() {
        return bountyPostId;
    }

    public void setBountyPostId(String bountyPostId) {
        this.bountyPostId = bountyPostId;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}