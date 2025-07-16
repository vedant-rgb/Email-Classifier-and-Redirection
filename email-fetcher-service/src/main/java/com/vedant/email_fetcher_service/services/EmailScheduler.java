package com.vedant.email_fetcher_service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EmailScheduler {
    private static final Logger logger = LoggerFactory.getLogger(EmailScheduler.class);

    private final InboxReader inboxReader;

    public EmailScheduler(InboxReader inboxReader) {
        this.inboxReader = inboxReader;
    }

    @Scheduled(fixedDelay = 20000) // Run every 20 seconds after the previous execution completes
    public void scheduleFetchEmails() {
        try {
            logger.info("Starting scheduled email fetch at {}", System.currentTimeMillis());
            inboxReader.fetchEmails();
            logger.info("Completed scheduled email fetch");
        } catch (Exception e) {
            logger.error("Error during scheduled email fetch", e);
        }
    }
}