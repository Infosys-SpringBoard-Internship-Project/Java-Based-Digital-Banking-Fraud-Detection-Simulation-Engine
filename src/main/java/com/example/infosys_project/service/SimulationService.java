package com.example.infosys_project.service;

import com.example.infosys_project.dto.SimulationRequest;
import com.example.infosys_project.dto.ValidationResponse;
import com.example.infosys_project.generator.TransactionGenerator;
import com.example.infosys_project.model.TransactionModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SimulationService {

    private final TransactionService transactionService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final List<Map<String, Object>> eventLog = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> recentTransactions = Collections.synchronizedList(new ArrayList<>());

    private volatile int totalPlanned = 0;
    private volatile int processed = 0;
    private volatile int fraudCount = 0;
    private volatile double averageRiskScore = 0.0;
    private volatile String status = "IDLE";
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime completedAt;
    private volatile SimulationRequest lastRequest;

    public SimulationService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public synchronized Map<String, Object> startSimulation(SimulationRequest request) {
        if (running.get()) {
            throw new RuntimeException("Simulation already running");
        }

        SimulationRequest safeRequest = sanitize(request);
        this.lastRequest = safeRequest;
        this.totalPlanned = safeRequest.getVolume();
        this.processed = 0;
        this.fraudCount = 0;
        this.averageRiskScore = 0.0;
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
        this.completedAt = null;
        this.eventLog.clear();
        this.recentTransactions.clear();
        this.running.set(true);
        this.stopRequested.set(false);

        appendEvent("Starting simulation", safeRequest.getVolume() + " txns / fraud target " + safeRequest.getFraudPercentage() + "%");
        CompletableFuture.runAsync(() -> runSimulation(safeRequest));
        return getStatus();
    }

    public synchronized Map<String, Object> stopSimulation() {
        if (!running.get()) {
            status = "STOPPED";
            return getStatus();
        }
        stopRequested.set(true);
        status = "STOPPING";
        appendEvent("Stop requested", "Simulation will halt after current transaction");
        return getStatus();
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("running", running.get());
        response.put("status", status);
        response.put("processed", processed);
        response.put("totalPlanned", totalPlanned);
        response.put("fraudCount", fraudCount);
        response.put("normalCount", Math.max(processed - fraudCount, 0));
        response.put("averageRiskScore", Math.round(averageRiskScore * 10.0) / 10.0);
        response.put("startedAt", startedAt);
        response.put("completedAt", completedAt);
        response.put("request", lastRequest);
        response.put("events", new ArrayList<>(eventLog));
        response.put("recentTransactions", new ArrayList<>(recentTransactions));
        return response;
    }

    private SimulationRequest sanitize(SimulationRequest request) {
        SimulationRequest safe = request == null ? new SimulationRequest() : request;
        if (safe.getVolume() == null || safe.getVolume() < 1) safe.setVolume(1);
        if (safe.getVolume() > 1000) safe.setVolume(1000);
        if (safe.getFraudPercentage() == null || safe.getFraudPercentage() < 0) safe.setFraudPercentage(0);
        if (safe.getFraudPercentage() > 100) safe.setFraudPercentage(100);
        if (safe.getMediumPercentage() == null || safe.getMediumPercentage() < 0) safe.setMediumPercentage(0);
        if (safe.getMediumPercentage() > 100) safe.setMediumPercentage(100);
        if (safe.getNormalPercentage() == null || safe.getNormalPercentage() < 0) safe.setNormalPercentage(0);
        if (safe.getNormalPercentage() > 100) safe.setNormalPercentage(100);

        int fraud = safe.getFraudPercentage();
        int medium = safe.getMediumPercentage();
        int normal = safe.getNormalPercentage();
        int sum = fraud + medium + normal;
        if (sum <= 0) {
            safe.setFraudPercentage(13);
            safe.setMediumPercentage(12);
            safe.setNormalPercentage(75);
        } else if (sum != 100) {
            int normalizedNormal = Math.max(0, Math.min(100, 100 - fraud - medium));
            safe.setNormalPercentage(normalizedNormal);
        }
        if (safe.getAmountMin() == null || safe.getAmountMin() < 0) safe.setAmountMin(0.0);
        if (safe.getAmountMax() == null || safe.getAmountMax() <= safe.getAmountMin()) safe.setAmountMax(Math.max(safe.getAmountMin() + 1000.0, 100000.0));
        if (safe.getExecutionMode() == null || safe.getExecutionMode().isBlank()) safe.setExecutionMode("INSTANT");
        if (safe.getDurationMinutes() == null || safe.getDurationMinutes() < 1) safe.setDurationMinutes(1);
        if (safe.getScenarios() == null) safe.setScenarios(new ArrayList<>());
        return safe;
    }

    protected void runSimulation(SimulationRequest request) {
        double cumulativeRisk = 0.0;
        int fraudTarget = (int) Math.round(request.getVolume() * (request.getFraudPercentage() / 100.0));
        int mediumTarget = (int) Math.round(request.getVolume() * (request.getMediumPercentage() / 100.0));
        int fraudRemaining = fraudTarget;
        int mediumRemaining = mediumTarget;
        int transactionsRemaining = request.getVolume();

        try {
            for (int i = 0; i < request.getVolume(); i++) {
                if (stopRequested.get()) {
                    status = "STOPPED";
                    appendEvent("Simulation stopped", processed + " transactions generated before halt");
                    break;
                }

                boolean shouldGenerateFraud = fraudRemaining > 0
                        && (transactionsRemaining == fraudRemaining || Math.random() < ((double) fraudRemaining / transactionsRemaining));
                boolean shouldGenerateMedium = !shouldGenerateFraud
                        && mediumRemaining > 0
                        && (transactionsRemaining == mediumRemaining || Math.random() < ((double) mediumRemaining / transactionsRemaining));
                TransactionModel model = TransactionGenerator.generateTransactionForScenario(
                        request.getScenarios(),
                        shouldGenerateFraud,
                        shouldGenerateMedium,
                        request.getAmountMin(),
                        request.getAmountMax()
                );
                ValidationResponse response = transactionService.processTransaction(model);
                transactionsRemaining -= 1;
                if (shouldGenerateFraud) {
                    fraudRemaining -= 1;
                } else if (shouldGenerateMedium) {
                    mediumRemaining -= 1;
                }

                processed += 1;
                if (response.transaction != null) {
                    cumulativeRisk += response.transaction.riskScore;
                    averageRiskScore = cumulativeRisk / processed;
                    if (response.transaction.isFraud) {
                        fraudCount += 1;
                    }
                    appendRecentTransaction(response.transaction, response.status);
                    appendEvent("Txn " + processed + "/" + totalPlanned,
                            response.transaction.accountHolderName + " · " + response.transaction.riskLevel + " · ₹" + Math.round(response.transaction.amount));
                }

                if ("GRADUAL".equalsIgnoreCase(request.getExecutionMode())) {
                    long sleepMs = Math.max(250L, (request.getDurationMinutes() * 60_000L) / Math.max(request.getVolume(), 1));
                    Thread.sleep(sleepMs);
                }
            }

            if (!stopRequested.get()) {
                status = "COMPLETED";
                appendEvent("Simulation completed", fraudCount + " flagged / avg risk " + Math.round(averageRiskScore * 10.0) / 10.0);
            }
        } catch (Exception ex) {
            status = "FAILED";
            appendEvent("Simulation failed", ex.getMessage());
        } finally {
            completedAt = LocalDateTime.now();
            running.set(false);
            stopRequested.set(false);
        }
    }

    private void appendEvent(String title, String detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("timestamp", LocalDateTime.now().toString());
        row.put("title", title);
        row.put("detail", detail);
        eventLog.add(0, row);
        while (eventLog.size() > 40) {
            eventLog.remove(eventLog.size() - 1);
        }
    }

    private void appendRecentTransaction(TransactionModel tx, String statusText) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", tx.transactionId);
        row.put("name", tx.accountHolderName);
        row.put("amount", tx.amount);
        row.put("riskLevel", tx.riskLevel);
        row.put("status", statusText);
        recentTransactions.add(0, row);
        while (recentTransactions.size() > 12) {
            recentTransactions.remove(recentTransactions.size() - 1);
        }
    }
}
