package com.example.infosys_project.service;

import com.example.infosys_project.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionRetentionService {

    private final TransactionRepository transactionRepository;

    public TransactionRetentionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void pruneOldTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);
        transactionRepository.deleteByTimestampBefore(cutoff);
    }
}
