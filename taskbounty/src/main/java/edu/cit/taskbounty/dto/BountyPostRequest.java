package edu.cit.taskbounty.dto;

import java.math.BigDecimal;

public class BountyPostRequest {
    private String title;
    private String description;
    private BigDecimal bountyPrice;

    // Constructors
    public BountyPostRequest() {}

    public BountyPostRequest(String title, String description, BigDecimal bountyPrice) {
        this.title = title;
        this.description = description;
        this.bountyPrice = bountyPrice;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBountyPrice() {
        return bountyPrice;
    }

    public void setBountyPrice(BigDecimal bountyPrice) {
        this.bountyPrice = bountyPrice;
    }
}