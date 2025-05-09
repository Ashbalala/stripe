package edu.cit.taskbounty.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "bounty_posts")
public class BountyPost {

    @Id
    private String id;
    private String creatorId; // Reference to the user who created the post
    private String title;
    private String description; // Markdown content
    private BigDecimal bountyPrice; // Total bounty price
    private boolean isPublic; // True after payment is confirmed
    private int upvotes;
    private int downvotes;
    private List<String> votedUp; // List of user IDs who upvoted
    private List<String> votedDown; // List of user IDs who downvoted
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    // Default constructor
    public BountyPost() {
        this.votedUp = new ArrayList<>();
        this.votedDown = new ArrayList<>();
    }

    // Parameterized constructor
    public BountyPost(String creatorId, String title, String description, BigDecimal bountyPrice, boolean isPublic) {
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.bountyPrice = bountyPrice;
        this.isPublic = isPublic;
        this.upvotes = 0;
        this.downvotes = 0;
        this.votedUp = new ArrayList<>();
        this.votedDown = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBountyPrice() { return bountyPrice; }
    public void setBountyPrice(BigDecimal bountyPrice) { this.bountyPrice = bountyPrice; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }
    public List<String> getVotedUp() { return votedUp; }
    public void setVotedUp(List<String> votedUp) { this.votedUp = votedUp; }
    public List<String> getVotedDown() { return votedDown; }
    public void setVotedDown(List<String> votedDown) { this.votedDown = votedDown; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void topUpBounty(BigDecimal amount) {
        this.bountyPrice = this.bountyPrice.add(amount);
    }
}