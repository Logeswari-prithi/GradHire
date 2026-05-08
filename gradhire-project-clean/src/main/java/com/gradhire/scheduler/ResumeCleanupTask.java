package com.gradhire.scheduler;

import com.gradhire.repository.ResumeHistoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ResumeCleanupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResumeCleanupTask.class);
    private final ResumeHistoryRepository resumeHistoryRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Run everyday at midnight
    @Transactional
    public void cleanupOldResumes() {
        log.info("Starting cleanup of old resumes");
        try {
            resumeHistoryRepository.deleteByExpirationDateBefore(LocalDateTime.now());
            log.info("Cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during resume cleanup: ", e);
        }
    }
}
