package edu.cit.taskbounty.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "comments")
public class Comment {

    @Id
    private String id;
    private String bountyPostId; // Links to the BountyPost
    private String parentCommentId; // Links to the parent comment (null for top-level)
    private String authorId; // User who wrote the comment
    private String content; // Comment text (Markdown)
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public Comment() {
    }

    public Comment(String id, String bountyPostId, String parentCommentId, String authorId, String content) {
        this.id = id;
        this.bountyPostId = bountyPostId;
        this.parentCommentId = parentCommentId;
        this.authorId = authorId;
        this.content = content;
    }

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