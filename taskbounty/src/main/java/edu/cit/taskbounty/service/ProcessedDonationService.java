package edu.cit.taskbounty.service;

import edu.cit.taskbounty.model.ProcessedDonation;
import edu.cit.taskbounty.repository.ProcessedDonationRepository;
import org.springframework.stereotype.Service;

@Service
public class ProcessedDonationService {
    private final ProcessedDonationRepository processedDonationRepository;

    public ProcessedDonationService(ProcessedDonationRepository processedDonationRepository) {
        this.processedDonationRepository = processedDonationRepository;
    }

    public boolean isProcessed(String sessionId) {
        return processedDonationRepository.existsBySessionId(sessionId);
    }

    public void markAsProcessed(String sessionId) {
        processedDonationRepository.save(new ProcessedDonation(sessionId));
    }
}
