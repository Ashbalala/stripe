package edu.cit.taskbounty.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "processedDonations")
public class ProcessedDonation {
    @Id
    private String sessionId;

    public ProcessedDonation(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
