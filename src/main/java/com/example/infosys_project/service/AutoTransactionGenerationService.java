package com.example.infosys_project.service;

import com.example.infosys_project.dto.ValidationResponse;
import com.example.infosys_project.generator.TransactionGenerator;
import com.example.infosys_project.model.TransactionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AutoTransactionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AutoTransactionGenerationService.class);
    private static final int MIN_INTERVAL_SECONDS = 10;
    private static final int MAX_INTERVAL_SECONDS = 120;

    private final TransactionService transactionService;

    private volatile LocalDateTime nextRunAt = LocalDateTime.now();
    private volatile LocalDateTime lastRunAt;
    private volatile String lastStatus = "IDLE";
    private volatile String lastMessage = "Waiting for next run";
    private volatile String lastTransactionId;
    private volatile boolean enabled = true;
    private volatile long generatedCount = 0;
    private volatile long rejectedCount = 0;
    private volatile long failureCount = 0;

    public AutoTransactionGenerationService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(fixedDelay = 10000)
    public void maybeGenerate() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(nextRunAt)) {
            return;
        }

        runGeneration(now);
    }

    private void runGeneration(LocalDateTime now) {
        try {
            TransactionModel tx = TransactionGenerator.generateTransaction();
            tx.timestamp = now;
            ValidationResponse response = transactionService.processTransaction(tx);
            lastRunAt = now;
            lastTransactionId = tx.transactionId;

            if (response != null && response.saved) {
                generatedCount += 1;
                lastStatus = "GENERATED";
                lastMessage = response.message == null ? "Transaction generated" : response.message;
            } else {
                rejectedCount += 1;
                lastStatus = response == null ? "REJECTED" : response.status;
                lastMessage = response == null ? "Transaction rejected" : response.message;
            }
        } catch (Exception ex) {
            lastRunAt = now;
            lastStatus = "FAILED";
            lastMessage = ex.getMessage();
            failureCount += 1;
            log.warn("Auto transaction generation failed", ex);
        } finally {
            nextRunAt = now.plusSeconds(randomDelaySeconds());
        }
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", enabled);
        data.put("intervalMinSec", MIN_INTERVAL_SECONDS);
        data.put("intervalMaxSec", MAX_INTERVAL_SECONDS);
        data.put("lastRunAt", lastRunAt);
        data.put("nextRunAt", nextRunAt);
        data.put("lastStatus", lastStatus);
        data.put("lastMessage", lastMessage);
        data.put("lastTransactionId", lastTransactionId);
        data.put("generatedCount", generatedCount);
        data.put("rejectedCount", rejectedCount);
        data.put("failureCount", failureCount);
        return data;
    }

    private int randomDelaySeconds() {
        return ThreadLocalRandom.current().nextInt(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS + 1);
    }

    public synchronized Map<String, Object> start() {
        enabled = true;
        nextRunAt = LocalDateTime.now();
        lastStatus = "RESUMED";
        lastMessage = "Auto generation resumed";
        return getStatus();
    }

    public synchronized Map<String, Object> stop() {
        enabled = false;
        lastStatus = "PAUSED";
        lastMessage = "Auto generation paused";
        return getStatus();
    }
}
