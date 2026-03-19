package com.example.infosys_project.service;

import com.example.infosys_project.detection.FraudDetector;
import com.example.infosys_project.dto.ValidationResponse;
import com.example.infosys_project.model.FraudAlert;
import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.repository.FraudAlertRepository;
import com.example.infosys_project.repository.TransactionRepository;
import com.example.infosys_project.validation.TransactionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EmailAlertService emailAlertService;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // Flask ML API endpoints.
    private static final String ML_API_URL = "http://localhost:5000/predict";
    private static final String ML_HEALTH_URL = "http://localhost:5000/health";

    public boolean isMlApiHealthy() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> health = restTemplate.getForObject(ML_HEALTH_URL, Map.class);
            return health != null && "running".equalsIgnoreCase(String.valueOf(health.get("status")));
        } catch (Exception e) {
            return false;
        }
    }

    public ValidationResponse processTransaction(TransactionModel tx) {

        // Fill missing system fields for manually submitted transactions.
        if (tx.transactionId == null || tx.transactionId.isBlank())
            tx.transactionId = UUID.randomUUID().toString();
        if (tx.utrNumber == null || tx.utrNumber.isBlank())
            tx.utrNumber = "UTR" + System.currentTimeMillis();
        if (tx.timestamp == null)
            tx.timestamp = LocalDateTime.now();
        if (tx.currency  == null) tx.currency  = "INR";
        if (tx.riskLevel == null) tx.riskLevel = "LOW";
        if (tx.ipRiskTag == null) tx.ipRiskTag = "CLEAN";
        if (tx.ipCountry == null) tx.ipCountry = "Unknown";

        ValidationResponse response = new ValidationResponse();
        response.transaction = tx;

        // Step 1: validate all transaction fields.
        String validationResult = TransactionValidator.validate(tx);
        response.validationResult = validationResult;

        if (!validationResult.equals("Valid Transaction Details")) {
            response.saved   = false;
            response.status  = "REJECTED";
            response.message = "Transaction rejected — " + validationResult;
            return response;
        }

        // Step 2: run rule-based fraud detection.
        FraudDetector.checkFraud(tx);

        // Step 3: run ML fraud scoring (skip if Flask is unavailable).
        tx.mlFraudProbability = callMlApi(tx);

        // Step 4: upgrade to fraud if ML confidence is high.
        if (tx.mlFraudProbability >= 0.75 && !tx.isFraud) {
            tx.isFraud   = true;
            tx.riskLevel = "HIGH";
            tx.riskScore = Math.max(tx.riskScore, 5.5);
            String mlTag = "R-ML:MLFlagged(prob=" + tx.mlFraudProbability + ")";
            tx.fraudReason = tx.fraudReason.equals("None")
                    ? mlTag : tx.fraudReason + " | " + mlTag;
        }

        // Step 5: save computed transaction.
        transactionRepository.save(tx);

        if ("HIGH".equalsIgnoreCase(tx.riskLevel) || "CRITICAL".equalsIgnoreCase(tx.riskLevel)) {
            emailAlertService.sendFraudAlert(tx);
        }

        if (tx.isFraud) {
            FraudAlert alert = new FraudAlert();
            alert.setTransactionId(tx.transactionId);
            alert.setAccountHolderName(tx.accountHolderName);
            alert.setAmount(tx.amount);
            alert.setRiskLevel(tx.riskLevel);
            alert.setRiskScore(tx.riskScore);
            alert.setFraudReason(tx.fraudReason);
            alert.setLocation(tx.location);
            alert.setIpRiskTag(tx.ipRiskTag);
            alert.setRead(false);
            fraudAlertRepository.save(alert);
        }

        response.saved = true;

        // Step 6: build a human-readable status response.
        switch (tx.riskLevel) {
            case "CRITICAL":
                response.status  = "FRAUD_DETECTED";
                response.message = "CRITICAL FRAUD [Score=" + tx.riskScore
                        + "] | " + tx.fraudReason;
                break;
            case "HIGH":
                response.status  = "FRAUD_DETECTED";
                response.message = "HIGH RISK FRAUD [Score=" + tx.riskScore
                        + "] | " + tx.fraudReason;
                break;
            case "MEDIUM":
                response.status  = "REVIEW_NEEDED";
                response.message = "Medium Risk [Score=" + tx.riskScore
                        + "] flagged for review | " + tx.fraudReason;
                break;
            default:
                response.status  = "CLEAN";
                response.message = "Clean transaction [Score=" + tx.riskScore + "]";
        }

        return response;
    }

    /**
     * Call Flask /predict and return fraud probability (0.0-1.0).
     * If Flask is unavailable, return 0.0 so the rule engine can still run.
     */
    private double callMlApi(TransactionModel tx) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount",                    tx.amount);
            payload.put("balance",                   tx.balance);
            payload.put("txn_count_last_1hr",        tx.txnCountLastHour);
            payload.put("txn_count_last_24hr",       tx.txnCountLast24Hours);
            payload.put("avg_txn_amount_30days",     tx.avgTxnAmount30Days);
            payload.put("distance_from_last_txn_km", tx.distanceFromLastTxnKm);
            payload.put("account_age_days",          tx.accountAgeDays);
            payload.put("is_new_location",           tx.isNewLocation    ? 1 : 0);
            payload.put("is_new_device",             tx.isNewDevice      ? 1 : 0);
            payload.put("is_vpn_or_proxy",           tx.isVpnOrProxy     ? 1 : 0);
            payload.put("ip_matches_location",       tx.ipMatchesLocation ? 1 : 0);
            payload.put("is_international",          tx.isInternational  ? 1 : 0);
            payload.put("is_first_time_receiver",    tx.isFirstTimeReceiver ? 1 : 0);
            payload.put("merchant_category",         tx.merchantCategory);
            payload.put("transaction_mode",          tx.transactionMode);
            payload.put("ip_risk_tag",               tx.ipRiskTag);

            @SuppressWarnings("unchecked")
            Map<String, Object> mlResp =
                    restTemplate.postForObject(ML_API_URL, payload, Map.class);

            if (mlResp != null && mlResp.containsKey("fraud_probability")) {
                return ((Number) mlResp.get("fraud_probability")).doubleValue();
            }
        } catch (Exception e) {
            System.out.println("[ML] Flask not available, skipping ML score: "
                               + e.getMessage());
        }
        return 0.0;
    }
}
