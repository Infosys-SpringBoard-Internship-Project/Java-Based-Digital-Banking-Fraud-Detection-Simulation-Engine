package com.example.infosys_project.controller;

import com.example.infosys_project.dto.ValidationResponse;
import com.example.infosys_project.generator.TransactionGenerator;
import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.repository.TransactionRepository;
import com.example.infosys_project.security.DataMaskingUtil;
import com.example.infosys_project.service.AuditService;
import com.example.infosys_project.service.AuthService;
import com.example.infosys_project.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
@RestController
@RequestMapping("/transaction")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;

    @GetMapping("/generate")
    public TransactionModel generate() {
        return TransactionGenerator.generateTransaction();
    }

    @GetMapping("/autoValidate")
    public ValidationResponse autoValidate() {
        return transactionService.processTransaction(
                TransactionGenerator.generateTransaction());
    }

    @GetMapping("/validate")
    public void validateForm(HttpServletResponse response) throws IOException {
        response.sendRedirect("/pages/dashboard.html");
    }

    @GetMapping("/dashboard")
    public void dashboard(HttpServletResponse response) throws IOException {
        response.sendRedirect("/pages/dashboard.html");
    }

    @PostMapping("/validate")
    public ValidationResponse validate(@RequestBody TransactionModel tx,
                                       @RequestHeader(value = "Authorization", required = false) String authorization,
                                       HttpServletRequest request) {
        ValidationResponse response = transactionService.processTransaction(tx);
        AdminUser actor = resolveActor(authorization);
        if (actor != null) {
            auditService.log(actor, "MANUAL_VALIDATE", "TRANSACTION", response.transaction != null ? response.transaction.transactionId : null,
                    "Manual transaction validation submitted", request.getRemoteAddr(), request.getHeader("User-Agent"));
        }
        return response;
    }

    @GetMapping("/all")
    public List<TransactionModel> getAll(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        List<TransactionModel> transactions = transactionRepository.findAllByOrderByTimestampDesc();
        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            return DataMaskingUtil.maskTransactions(transactions);
        }
        return transactions;
    }

    @GetMapping("/search")
    public Map<String, Object> searchTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Double amountMin,
            @RequestParam(required = false) Double amountMax,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String riskLevel,
            @RequestParam(defaultValue = "all") String fraudType,
            @RequestParam(required = false) String account,
            @RequestParam(defaultValue = "all") String ipRiskTag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> result = transactionService.searchTransactions(
                query,
                dateFrom,
                dateTo,
                amountMin,
                amountMax,
                status,
                riskLevel,
                fraudType,
                account,
                ipRiskTag,
                page,
                size
        );

        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            if (items != null) {
                result.put("items", DataMaskingUtil.maskTransactions(items));
            }
        }

        return result;
    }

    @GetMapping("/{id}")
    public TransactionModel getById(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TransactionModel transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            return DataMaskingUtil.maskTransaction(transaction);
        }
        return transaction;
    }

    @GetMapping("/detail/{id}")
    public Map<String, Object> getDetail(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> detail = transactionService.getTransactionDetail(id);
        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            detail.put("transaction", DataMaskingUtil.maskTransaction((TransactionModel) detail.get("transaction")));
            @SuppressWarnings("unchecked")
            List<TransactionModel> history = (List<TransactionModel>) detail.get("history");
            @SuppressWarnings("unchecked")
            List<TransactionModel> related = (List<TransactionModel>) detail.get("related");
            detail.put("history", DataMaskingUtil.maskTransactions(history));
            detail.put("related", DataMaskingUtil.maskTransactions(related));
        }
        return detail;
    }

    @GetMapping("/frauds")
    public List<TransactionModel> getFrauds(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        List<TransactionModel> frauds = transactionRepository.findByIsFraudTrue();
        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            return DataMaskingUtil.maskTransactions(frauds);
        }
        return frauds;
    }

    @GetMapping("/by-risk/{level}")
    public List<TransactionModel> getByRiskLevel(
            @PathVariable String level,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String normalized = level == null ? "" : level.trim().toUpperCase();
        List<TransactionModel> transactions;
        if ("NORMAL".equals(normalized)) {
            transactions = transactionRepository.findByRiskLevelInIgnoreCase(Arrays.asList("NORMAL", "LOW"));
        } else {
            transactions = transactionRepository.findByRiskLevelIgnoreCase(normalized);
        }
        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            return DataMaskingUtil.maskTransactions(transactions);
        }
        return transactions;
    }

    @GetMapping("/by-ip-tag/{tag}")
    public List<TransactionModel> getByIpTag(
            @PathVariable String tag,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        List<TransactionModel> transactions = transactionRepository.findByIpRiskTagIgnoreCase(tag);
        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            return DataMaskingUtil.maskTransactions(transactions);
        }
        return transactions;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        long total    = transactionRepository.count();
        long fraud    = transactionRepository.countByIsFraudTrue();
        long critical = transactionRepository.countByRiskLevelIgnoreCase("CRITICAL");
        long high     = transactionRepository.countByRiskLevelIgnoreCase("HIGH");
        long medium   = transactionRepository.countByRiskLevelIgnoreCase("MEDIUM");
        long normal   = transactionRepository.countByRiskLevelInIgnoreCase(Arrays.asList("NORMAL", "LOW"));
        long vpn      = transactionRepository.countByIsVpnOrProxyTrue();
        long tor      = transactionRepository.countByIpRiskTagIgnoreCase("TOR");
        long ipMismatch = transactionRepository.countByIpMatchesLocationFalse();

        Double totalAmtObj  = transactionRepository.sumAmount();
        double totalAmt = totalAmtObj != null ? totalAmtObj : 0.0;

        Double fraudAmtObj  = transactionRepository.sumAmountByIsFraudTrue();
        double fraudAmt = fraudAmtObj != null ? fraudAmtObj : 0.0;

        Double avgRiskObj   = transactionRepository.avgRiskScore();
        double avgRisk = avgRiskObj != null ? avgRiskObj : 0.0;

        double fraudRate = total > 0
                ? Math.round(fraud * 10000.0 / total) / 100.0 : 0.0;

        Map<String, Long> ruleCounts = new LinkedHashMap<>();
        List<String> reasons = transactionRepository.findAllFraudReasons();
        for (String reason : reasons) {
            for (String part : reason.split("\\|")) {
                String code = part.trim().split(":")[0];
                ruleCounts.merge(code, 1L, Long::sum);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTransactions",  total);
        summary.put("fraudCount",         fraud);
        summary.put("fraudRate%",         fraudRate);
        summary.put("critical",           critical);
        summary.put("high",               high);
        summary.put("medium",             medium);
        summary.put("normal",             normal);
        summary.put("vpnDetected",        vpn);
        summary.put("torDetected",        tor);
        summary.put("ipLocationMismatch", ipMismatch);
        summary.put("totalAmountINR",     Math.round(totalAmt));
        summary.put("fraudAmountINR",     Math.round(fraudAmt));
        summary.put("avgRiskScore",       Math.round(avgRisk * 10.0) / 10.0);
        summary.put("ruleBreakdown",      ruleCounts);
        return summary;
    }

    @GetMapping("/analytics")
    public Map<String, Object> getAnalytics(@RequestParam(defaultValue = "30") int days) {
        return transactionService.getAnalyticsData(days);
    }

    @GetMapping("/system-status")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("springBoot", "UP");
        status.put("mlApi", transactionService.isMlApiHealthy() ? "UP" : "DOWN");
        status.put("timestamp", new Date().toString());
        return status;
    }

    @GetMapping("/export-csv")
    public void exportCsv(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=transactions.csv");

        List<TransactionModel> all = transactionRepository.findAll();

        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            all = DataMaskingUtil.maskTransactions(all);
        }

        PrintWriter w = response.getWriter();

        w.println("transaction_id,amount,balance,currency,merchant_category," +
                  "transaction_mode,location,is_new_location," +
                  "distance_from_last_txn_km,device,is_new_device," +
                  "is_vpn_or_proxy,ip_matches_location,ip_risk_tag," +
                  "txn_count_last_1hr,txn_count_last_24hr," +
                  "avg_txn_amount_30days,account_age_days," +
                  "is_international,is_first_time_receiver," +
                  "risk_score,risk_level,ml_fraud_probability,is_fraud");

        for (TransactionModel t : all) {
            w.println(
                t.transactionId         + "," + t.amount               + ","
                + t.balance             + "," + t.currency             + ","
                + t.merchantCategory    + "," + t.transactionMode      + ","
                + t.location            + "," + b(t.isNewLocation)     + ","
                + t.distanceFromLastTxnKm + "," + t.device             + ","
                + b(t.isNewDevice)      + "," + b(t.isVpnOrProxy)      + ","
                + b(t.ipMatchesLocation)+ "," + t.ipRiskTag            + ","
                + t.txnCountLastHour    + "," + t.txnCountLast24Hours  + ","
                + t.avgTxnAmount30Days  + "," + t.accountAgeDays       + ","
                + b(t.isInternational)  + "," + b(t.isFirstTimeReceiver) + ","
                + t.riskScore           + "," + t.riskLevel            + ","
                + t.mlFraudProbability  + "," + b(t.isFraud)
            );
        }
        w.flush();
    }

    @GetMapping("/export-csv/search")
    public void exportFilteredCsv(@RequestParam(required = false) String query,
                                  @RequestParam(required = false) String dateFrom,
                                  @RequestParam(required = false) String dateTo,
                                  @RequestParam(required = false) Double amountMin,
                                  @RequestParam(required = false) Double amountMax,
                                  @RequestParam(defaultValue = "all") String status,
                                  @RequestParam(defaultValue = "all") String riskLevel,
                                  @RequestParam(defaultValue = "all") String fraudType,
                                  @RequestParam(required = false) String account,
                                  @RequestParam(defaultValue = "all") String ipRiskTag,
                                  @RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions-filtered.csv");

        Map<String, Object> result = transactionService.searchTransactions(
                query, dateFrom, dateTo, amountMin, amountMax, status, riskLevel, fraudType, account, ipRiskTag, 0, 100000
        );

        @SuppressWarnings("unchecked")
        List<TransactionModel> items = (List<TransactionModel>) result.getOrDefault("items", Collections.emptyList());

        AdminUser actor = resolveActor(authorization);
        if (actor != null && DataMaskingUtil.shouldMaskData(actor.getRole())) {
            items = DataMaskingUtil.maskTransactions(items);
        }

        PrintWriter w = response.getWriter();
        w.println("transaction_id,account_holder_name,sender_account,receiver_account,amount,risk_level,is_fraud,fraud_reason,location,ip_risk_tag,timestamp");
        for (TransactionModel t : items) {
            w.println(csv(t.transactionId) + ","
                    + csv(t.accountHolderName) + ","
                    + csv(t.senderAccount) + ","
                    + csv(t.receiverAccount) + ","
                    + t.amount + ","
                    + csv(t.riskLevel) + ","
                    + b(t.isFraud) + ","
                    + csv(t.fraudReason) + ","
                    + csv(t.location) + ","
                    + csv(t.ipRiskTag) + ","
                    + csv(t.timestamp == null ? null : t.timestamp.toString()));
        }
        w.flush();

        if (actor != null) {
            auditService.log(actor, "EXPORT_FILTERED_CSV", "TRANSACTION", null,
                    "Exported filtered transaction CSV", request.getRemoteAddr(), request.getHeader("User-Agent"));
        }
    }

    private int b(boolean v) { return v ? 1 : 0; }

    private AdminUser resolveActor(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authService.getAdminFromToken(authorization.substring(7).trim()).orElse(null);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
