package com.example.infosys_project.service;

import com.example.infosys_project.detection.FraudDetector;
import com.example.infosys_project.dto.ValidationResponse;
import com.example.infosys_project.model.FraudAlert;
import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.repository.FraudAlertRepository;
import com.example.infosys_project.repository.TransactionRepository;
import com.example.infosys_project.validation.TransactionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EmailAlertService emailAlertService;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ml.api.url:http://127.0.0.1:5000/predict}")
    private String mlApiUrl;

    @Value("${ml.health.url:http://127.0.0.1:5000/health}")
    private String mlHealthUrl;

    public boolean isMlApiHealthy() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> health = restTemplate.getForObject(normalizeMlUrl(mlHealthUrl), Map.class);
            return health != null && "running".equalsIgnoreCase(String.valueOf(health.get("status")));
        } catch (Exception e) {
            System.out.println("[ML] Health check failed: " + e.getMessage());
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
        if (tx.riskLevel == null) tx.riskLevel = "NORMAL";
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

    public Map<String, Object> searchTransactions(String query,
                                                  String dateFrom,
                                                  String dateTo,
                                                  Double amountMin,
                                                  Double amountMax,
                                                  String status,
                                                  String riskLevel,
                                                  String fraudType,
                                                  String account,
                                                  String ipRiskTag,
                                                  int page,
                                                  int size) {
        List<TransactionModel> filtered = new ArrayList<>(transactionRepository.findAll());

        String normalizedQuery = normalize(query);
        String normalizedAccount = normalize(account);
        String normalizedStatus = normalize(status);
        String normalizedRiskLevel = normalize(riskLevel);
        String normalizedFraudType = normalize(fraudType);
        String normalizedIpRiskTag = normalize(ipRiskTag);

        LocalDateTime fromDateTime = parseDateStart(dateFrom);
        LocalDateTime toDateTime = parseDateEnd(dateTo);

        filtered = filtered.stream()
                .filter(tx -> matchesQuery(tx, normalizedQuery))
                .filter(tx -> matchesAccount(tx, normalizedAccount))
                .filter(tx -> matchesDateFrom(tx, fromDateTime))
                .filter(tx -> matchesDateTo(tx, toDateTime))
                .filter(tx -> matchesAmountMin(tx, amountMin))
                .filter(tx -> matchesAmountMax(tx, amountMax))
                .filter(tx -> matchesStatus(tx, normalizedStatus))
                .filter(tx -> matchesRiskLevel(tx, normalizedRiskLevel))
                .filter(tx -> matchesFraudType(tx, normalizedFraudType))
                .filter(tx -> matchesIpRiskTag(tx, normalizedIpRiskTag))
                .sorted(Comparator.comparing((TransactionModel tx) -> tx.timestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        int safeSize = size <= 0 ? 50 : Math.min(size, 500);
        int safePage = Math.max(page, 0);
        int total = filtered.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<TransactionModel> pageItems = filtered.subList(fromIndex, toIndex);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", pageItems);
        response.put("total", total);
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("totalPages", total == 0 ? 0 : (int) Math.ceil((double) total / safeSize));
        return response;
    }

    public Map<String, Object> getAnalyticsData(int days) {
        int safeDays = days <= 0 ? 30 : Math.min(days, 365);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(safeDays - 1L);

        List<TransactionModel> all = transactionRepository.findAll();
        List<TransactionModel> inWindow = all.stream()
                .filter(tx -> tx.timestamp != null)
                .filter(tx -> !tx.timestamp.toLocalDate().isBefore(startDate))
                .collect(Collectors.toList());

        Map<LocalDate, long[]> dailySeries = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = startDate.plusDays(i);
            dailySeries.put(date, new long[]{0L, 0L});
        }

        Map<String, Long> riskDistribution = new LinkedHashMap<>();
        riskDistribution.put("NORMAL", 0L);
        riskDistribution.put("MEDIUM", 0L);
        riskDistribution.put("HIGH", 0L);
        riskDistribution.put("CRITICAL", 0L);

        Map<String, Long> ipDistribution = new LinkedHashMap<>();
        Map<String, Long> merchantDistribution = new LinkedHashMap<>();
        Map<String, Long> ruleDistribution = new LinkedHashMap<>();

        double totalAmount = 0.0;
        double fraudAmount = 0.0;
        double totalRiskScore = 0.0;

        for (TransactionModel tx : inWindow) {
            LocalDate date = tx.timestamp.toLocalDate();
            long[] bucket = dailySeries.get(date);
            if (bucket != null) {
                bucket[0] += 1;
                if (tx.isFraud) {
                    bucket[1] += 1;
                }
            }

            String risk = tx.riskLevel == null ? "NORMAL" : tx.riskLevel.toUpperCase();
            if ("LOW".equals(risk)) {
                risk = "NORMAL";
            }
            riskDistribution.merge(risk, 1L, Long::sum);

            String ipTag = tx.ipRiskTag == null || tx.ipRiskTag.isBlank() ? "CLEAN" : tx.ipRiskTag.toUpperCase();
            ipDistribution.merge(ipTag, 1L, Long::sum);

            String merchant = tx.merchantCategory == null || tx.merchantCategory.isBlank()
                    ? "unknown"
                    : tx.merchantCategory.toLowerCase();
            merchantDistribution.merge(merchant, 1L, Long::sum);

            if (tx.fraudReason != null && !tx.fraudReason.isBlank() && !"None".equalsIgnoreCase(tx.fraudReason)) {
                for (String part : tx.fraudReason.split("\\|")) {
                    String code = part.trim().split(":")[0].trim();
                    if (!code.isEmpty()) {
                        ruleDistribution.merge(code, 1L, Long::sum);
                    }
                }
            }

            totalAmount += tx.amount;
            if (tx.isFraud) {
                fraudAmount += tx.amount;
            }
            totalRiskScore += tx.riskScore;
        }

        long totalTransactions = inWindow.size();
        long fraudCount = inWindow.stream().filter(tx -> tx.isFraud).count();
        double fraudRate = totalTransactions == 0 ? 0.0 : Math.round((fraudCount * 10000.0) / totalTransactions) / 100.0;
        double averageRisk = totalTransactions == 0 ? 0.0 : Math.round((totalRiskScore / totalTransactions) * 10.0) / 10.0;

        List<Map<String, Object>> trendSeries = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> entry : dailySeries.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", entry.getKey().toString());
            point.put("total", entry.getValue()[0]);
            point.put("fraud", entry.getValue()[1]);
            trendSeries.add(point);
        }

        List<Map<String, Object>> topMerchantDistribution = mapToSeries(merchantDistribution, 6);
        List<Map<String, Object>> topRuleDistribution = mapToSeries(ruleDistribution, 10);
        List<Map<String, Object>> ipSeries = mapToSeries(ipDistribution, 10);
        List<Map<String, Object>> riskSeries = riskDistribution.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("label", entry.getKey());
                    row.put("value", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("days", safeDays);
        summary.put("totalTransactions", totalTransactions);
        summary.put("fraudCount", fraudCount);
        summary.put("fraudRate", fraudRate);
        summary.put("totalAmount", Math.round(totalAmount));
        summary.put("fraudAmount", Math.round(fraudAmount));
        summary.put("averageRiskScore", averageRisk);

        Map<String, Object> comparison = buildComparison(all, safeDays);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("trendSeries", trendSeries);
        response.put("riskDistribution", riskSeries);
        response.put("ipDistribution", ipSeries);
        response.put("merchantDistribution", topMerchantDistribution);
        response.put("ruleDistribution", topRuleDistribution);
        response.put("comparison", comparison);
        return response;
    }

    public Map<String, Object> getTransactionDetail(String transactionId) {
        TransactionModel transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        List<TransactionModel> all = transactionRepository.findAll();

        List<TransactionModel> history = all.stream()
                .filter(tx -> tx.accountHolderName != null && tx.accountHolderName.equalsIgnoreCase(transaction.accountHolderName))
                .sorted(Comparator.comparing((TransactionModel tx) -> tx.timestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<TransactionModel> related = all.stream()
                .filter(tx -> !tx.transactionId.equals(transaction.transactionId))
                .filter(tx -> (tx.accountHolderName != null && tx.accountHolderName.equalsIgnoreCase(transaction.accountHolderName))
                        || (tx.receiverAccount != null && tx.receiverAccount.equals(transaction.receiverAccount))
                        || Math.abs(tx.amount - transaction.amount) <= Math.max(1000.0, transaction.amount * 0.12))
                .sorted(Comparator.comparing((TransactionModel tx) -> tx.timestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(8)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("transaction", transaction);
        response.put("ruleBreakdown", explainRules(transaction.fraudReason));
        response.put("history", history);
        response.put("related", related);
        return response;
    }

    private List<Map<String, String>> explainRules(String fraudReason) {
        Map<String, String> explanationMap = new LinkedHashMap<>();
        explanationMap.put("R01", "High-value transfer crossed configured amount thresholds.");
        explanationMap.put("R02", "Transaction occurred during suspicious late-night hours.");
        explanationMap.put("R03", "Transfer drained an unusually large portion of balance.");
        explanationMap.put("R04", "Velocity spike shows rapid-fire usage in a short window.");
        explanationMap.put("R05", "Merchant category is considered high risk.");
        explanationMap.put("R06", "Location jump is inconsistent with recent activity.");
        explanationMap.put("R07", "New device fingerprint detected for the account.");
        explanationMap.put("R08", "IP intelligence flagged risky network behavior.");
        explanationMap.put("R09", "International pattern deviates from expected behavior.");
        explanationMap.put("R10", "New beneficiary combined with elevated amount.");
        explanationMap.put("R11", "Amount is far above the recent average baseline.");
        explanationMap.put("R12", "New account is performing an aggressive transaction.");
        explanationMap.put("R13", "Daily volume exceeds normal behavior for the account.");
        explanationMap.put("R14", "Rounded amount resembles structured fraud behavior.");
        explanationMap.put("R-ML", "ML model raised the risk above the rule-only result.");

        if (fraudReason == null || fraudReason.isBlank() || "None".equalsIgnoreCase(fraudReason)) {
            return List.of();
        }

        return Arrays.stream(fraudReason.split("\\|"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> {
                    String code = part.split(":")[0].trim();
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("code", code);
                    row.put("trigger", part);
                    row.put("explanation", explanationMap.getOrDefault(code, "Fraud detector raised a custom signal for this transaction."));
                    return row;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildComparison(List<TransactionModel> all, int days) {
        LocalDateTime currentStart = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        LocalDateTime previousStart = currentStart.minusDays(days);
        LocalDateTime previousEnd = currentStart.minusNanos(1);

        List<TransactionModel> current = all.stream()
                .filter(tx -> tx.timestamp != null && !tx.timestamp.isBefore(currentStart))
                .collect(Collectors.toList());

        List<TransactionModel> previous = all.stream()
                .filter(tx -> tx.timestamp != null && !tx.timestamp.isBefore(previousStart) && !tx.timestamp.isAfter(previousEnd))
                .collect(Collectors.toList());

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("transactionDelta", computeDelta(current.size(), previous.size()));
        comparison.put("fraudDelta", computeDelta(current.stream().filter(tx -> tx.isFraud).count(), previous.stream().filter(tx -> tx.isFraud).count()));
        comparison.put("amountDelta", computeDelta(Math.round(current.stream().mapToDouble(tx -> tx.amount).sum()), Math.round(previous.stream().mapToDouble(tx -> tx.amount).sum())));
        comparison.put("riskDelta", computeDelta(
                Math.round(current.stream().mapToDouble(tx -> tx.riskScore).average().orElse(0.0) * 10.0),
                Math.round(previous.stream().mapToDouble(tx -> tx.riskScore).average().orElse(0.0) * 10.0)
        ));
        return comparison;
    }

    private Map<String, Object> computeDelta(long currentValue, long previousValue) {
        double delta = previousValue == 0
                ? (currentValue == 0 ? 0.0 : 100.0)
                : ((currentValue - previousValue) * 100.0) / previousValue;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", currentValue);
        result.put("previous", previousValue);
        result.put("delta", Math.round(delta * 10.0) / 10.0);
        return result;
    }

    private List<Map<String, Object>> mapToSeries(Map<String, Long> source, int limit) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("label", entry.getKey());
                    row.put("value", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean matchesQuery(TransactionModel tx, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(tx.accountHolderName, query)
                || contains(tx.senderAccount, query)
                || contains(tx.receiverAccount, query)
                || contains(tx.transactionId, query)
                || contains(tx.merchantCategory, query)
                || contains(tx.location, query);
    }

    private boolean matchesAccount(TransactionModel tx, String account) {
        if (account.isEmpty()) {
            return true;
        }
        return contains(tx.senderAccount, account) || contains(tx.receiverAccount, account);
    }

    private boolean matchesDateFrom(TransactionModel tx, LocalDateTime fromDateTime) {
        return fromDateTime == null || (tx.timestamp != null && !tx.timestamp.isBefore(fromDateTime));
    }

    private boolean matchesDateTo(TransactionModel tx, LocalDateTime toDateTime) {
        return toDateTime == null || (tx.timestamp != null && !tx.timestamp.isAfter(toDateTime));
    }

    private boolean matchesAmountMin(TransactionModel tx, Double amountMin) {
        return amountMin == null || tx.amount >= amountMin;
    }

    private boolean matchesAmountMax(TransactionModel tx, Double amountMax) {
        return amountMax == null || tx.amount <= amountMax;
    }

    private boolean matchesStatus(TransactionModel tx, String status) {
        if (status.isEmpty() || "all".equals(status)) {
            return true;
        }
        if ("fraud".equals(status)) {
            return tx.isFraud;
        }
        if ("non_fraud".equals(status) || "clean".equals(status)) {
            return !tx.isFraud;
        }
        return true;
    }

    private boolean matchesRiskLevel(TransactionModel tx, String riskLevel) {
        if (riskLevel.isEmpty() || "all".equals(riskLevel)) {
            return true;
        }
        if (tx.riskLevel == null) {
            return "normal".equals(riskLevel);
        }
        if ("normal".equals(riskLevel)) {
            return tx.riskLevel.equalsIgnoreCase("normal") || tx.riskLevel.equalsIgnoreCase("low");
        }
        return tx.riskLevel.equalsIgnoreCase(riskLevel);
    }

    private boolean matchesFraudType(TransactionModel tx, String fraudType) {
        if (fraudType.isEmpty() || "all".equals(fraudType)) {
            return true;
        }
        return tx.fraudReason != null && tx.fraudReason.toLowerCase().contains(fraudType);
    }

    private boolean matchesIpRiskTag(TransactionModel tx, String ipRiskTag) {
        if (ipRiskTag.isEmpty() || "all".equals(ipRiskTag)) {
            return true;
        }
        return tx.ipRiskTag != null && tx.ipRiskTag.equalsIgnoreCase(ipRiskTag);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private LocalDateTime parseDateStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atStartOfDay();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atTime(LocalTime.MAX);
        } catch (DateTimeParseException ex) {
            return null;
        }
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
                    restTemplate.postForObject(normalizeMlUrl(mlApiUrl), payload, Map.class);

            if (mlResp != null && mlResp.containsKey("fraud_probability")) {
                return ((Number) mlResp.get("fraud_probability")).doubleValue();
            }
        } catch (Exception e) {
            System.out.println("[ML] Flask not available, skipping ML score: "
                               + e.getMessage());
        }
        return 0.0;
    }

    private String normalizeMlUrl(String raw) {
        if (raw == null) {
            return "http://127.0.0.1:5000/health";
        }
        String value = raw.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }
}
