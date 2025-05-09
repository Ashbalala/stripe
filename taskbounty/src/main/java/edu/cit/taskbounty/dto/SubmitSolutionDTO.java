package edu.cit.taskbounty.dto;

import org.springframework.web.multipart.MultipartFile;

public class SubmitSolutionDTO {

    private String bountyPostId;
    private String description;

    SubmitSolutionDTO(){}

    public String getBountyPostId() {
        return bountyPostId;
    }

    public void setBountyPostId(String bountyPostId) {
        this.bountyPostId = bountyPostId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
