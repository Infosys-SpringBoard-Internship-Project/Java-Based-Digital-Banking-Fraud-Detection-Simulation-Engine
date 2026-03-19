package com.example.infosys_project.controller;

import com.example.infosys_project.dto.ValidationResponse;
import com.example.infosys_project.generator.TransactionGenerator;
import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.repository.TransactionRepository;
import com.example.infosys_project.service.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/transaction")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    // Generate and return a random transaction (no save, no fraud check)
    @GetMapping("/generate")
    public TransactionModel generate() {
        return TransactionGenerator.generateTransaction();
    }

    // Full pipeline: Generate → Validate → Fraud Check → ML → Save
    @GetMapping("/autoValidate")
    public ValidationResponse autoValidate() {
        return transactionService.processTransaction(
                TransactionGenerator.generateTransaction());
    }

    // Redirect to HTML form
    @GetMapping("/validate")
    public void validateForm(HttpServletResponse response) throws IOException {
        response.sendRedirect("/pages/dashboard.html");
    }

    // Redirect shortcut for dashboard view
    @GetMapping("/dashboard")
    public void dashboard(HttpServletResponse response) throws IOException {
        response.sendRedirect("/pages/dashboard.html");
    }

    // Manual POST: Submit your own JSON → full pipeline → save
    @PostMapping("/validate")
    public ValidationResponse validate(@RequestBody TransactionModel tx) {
        return transactionService.processTransaction(tx);
    }

    // Get all transactions
    @GetMapping("/all")
    public List<TransactionModel> getAll() {
        return transactionRepository.findAll();
    }

    // Get single transaction by UUID
    @GetMapping("/{id}")
    public TransactionModel getById(@PathVariable String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    // Get only fraud transactions
    @GetMapping("/frauds")
    public List<TransactionModel> getFrauds() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.isFraud)
                .collect(Collectors.toList());
    }

    // Filter by risk level: LOW | MEDIUM | HIGH | CRITICAL
    @GetMapping("/by-risk/{level}")
    public List<TransactionModel> getByRiskLevel(@PathVariable String level) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.riskLevel.equalsIgnoreCase(level))
                .collect(Collectors.toList());
    }

    // Filter by IP risk tag: CLEAN | VPN | PROXY | TOR | DATACENTER
    @GetMapping("/by-ip-tag/{tag}")
    public List<TransactionModel> getByIpTag(@PathVariable String tag) {
        return transactionRepository.findAll().stream()
                .filter(t -> tag.equalsIgnoreCase(t.ipRiskTag))
                .collect(Collectors.toList());
    }

    // Summary metrics from one DB fetch, computed in memory.
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        List<TransactionModel> all = transactionRepository.findAll();

        long total    = all.size();
        long fraud    = all.stream().filter(t -> t.isFraud).count();
        long critical = all.stream().filter(t -> "CRITICAL".equals(t.riskLevel)).count();
        long high     = all.stream().filter(t -> "HIGH".equals(t.riskLevel)).count();
        long medium   = all.stream().filter(t -> "MEDIUM".equals(t.riskLevel)).count();
        long low      = all.stream().filter(t -> "LOW".equals(t.riskLevel)).count();
        long vpn      = all.stream().filter(t -> t.isVpnOrProxy).count();
        long tor      = all.stream().filter(t -> "TOR".equalsIgnoreCase(t.ipRiskTag)).count();
        long ipMismatch = all.stream().filter(t -> !t.ipMatchesLocation).count();

        double totalAmt  = all.stream().mapToDouble(t -> t.amount).sum();
        double fraudAmt  = all.stream().filter(t -> t.isFraud).mapToDouble(t -> t.amount).sum();
        double avgRisk   = all.stream().mapToDouble(t -> t.riskScore).average().orElse(0.0);
        double fraudRate = total > 0
                ? Math.round(fraud * 10000.0 / total) / 100.0 : 0.0;

        // Rule breakdown: count rule codes from fraudReason strings.
        Map<String, Long> ruleCounts = new LinkedHashMap<>();
        for (TransactionModel t : all) {
            if (t.fraudReason == null || t.fraudReason.equals("None")) continue;
            for (String part : t.fraudReason.split("\\|")) {
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
        summary.put("low",                low);
        summary.put("vpnDetected",        vpn);
        summary.put("torDetected",        tor);
        summary.put("ipLocationMismatch", ipMismatch);
        summary.put("totalAmountINR",     Math.round(totalAmt));
        summary.put("fraudAmountINR",     Math.round(fraudAmt));
        summary.put("avgRiskScore",       Math.round(avgRisk * 10.0) / 10.0);
        summary.put("ruleBreakdown",      ruleCounts);
        return summary;
    }

    // Health info for dashboard status badge
    @GetMapping("/system-status")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("springBoot", "UP");
        status.put("mlApi", transactionService.isMlApiHealthy() ? "UP" : "DOWN");
        status.put("timestamp", new Date().toString());
        return status;
    }

    // Export all transactions as CSV for ML training
    @GetMapping("/export-csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=transactions.csv");

        List<TransactionModel> all = transactionRepository.findAll();
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

    private int b(boolean v) { return v ? 1 : 0; }
}
