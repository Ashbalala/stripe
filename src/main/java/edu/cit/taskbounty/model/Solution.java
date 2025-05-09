package edu.cit.taskbounty.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "solutions")
public class Solution {

    @Id
    private String id;

    private String bountyPostId;

    // The ID of the user who submitted the solution
    private String submitterId;

    private String content;

    // Flag to indicate if the bounty post creator approved this solution
    private boolean approved;

    // Timestamp of when the solution was submitted
    @CreatedDate
    private LocalDateTime createdAt;

    public Solution() {
    }

    public Solution(String id, String bountyPostId, String submitterId, String content, boolean approved, LocalDateTime createdAt) {
        this.id = id;
        this.bountyPostId = bountyPostId;
        this.submitterId = submitterId;
        this.content = content;
        this.approved = approved;
        this.createdAt = createdAt;
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

    public String getSubmitterId() {
        return submitterId;
    }

    public void setSubmitterId(String submitterId) {
        this.submitterId = submitterId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
