package com.example.infosys_project.service;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.model.UserRole;
import com.example.infosys_project.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailAlertService {

    private static final Logger log = LoggerFactory.getLogger(EmailAlertService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AdminRepository adminRepository;

    @Value("${mail.sender.address}")
    private String senderAddress;

    @Value("${mail.sender.name}")
    private String senderName;

    @PostConstruct
    public void init() {
        log.info("Mail sender configured: {}", senderAddress);
    }

    @Async
    public void sendFraudAlert(TransactionModel tx) {
        try {
            List<AdminUser> eligibleRecipients = adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue()
                    .stream()
                    .filter(admin -> admin.getRole() == UserRole.SUPERADMIN || admin.getRole() == UserRole.ADMIN)
                    .toList();

            if (eligibleRecipients.isEmpty()) {
                log.warn("No active admins found, skipping email alert");
                return;
            }

            // Step 2 — Build subject line
            String riskLevel = tx.riskLevel == null ? "HIGH" : tx.riskLevel.toUpperCase();
            String subject;
            if ("CRITICAL".equals(riskLevel)) {
                subject = "⚠ CRITICAL FRAUD ALERT — ₹" + formatAmount(tx.amount) + " | " + safe(tx.accountHolderName);
            } else {
                subject = "🔶 HIGH RISK ALERT — ₹" + formatAmount(tx.amount) + " | " + safe(tx.accountHolderName);
            }

            // Step 3 — Build HTML body
            String htmlBody = buildEmailHtml(tx);

            String fromAddress = senderName + " <" + senderAddress + ">";
            for (AdminUser admin : eligibleRecipients) {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setFrom(fromAddress);
                    helper.setTo(admin.getEmail());
                    helper.setSubject(subject + " [" + admin.getRole().name() + "]");
                    helper.setText(buildRoleAwareEmailHtml(admin, tx), true);
                    mailSender.send(message);
                    log.info("Alert sent to {} for txn {}", admin.getEmail(), safe(tx.transactionId));
                } catch (Exception e) {
                    log.error("Failed to send alert to {}: {}", admin.getEmail(), e.getMessage());
                }
            }

        } catch (Exception e) {
            // Step 5 — Never let exceptions propagate
            log.error("Failed to send fraud alert email: {}", e.getMessage(), e);
        }
    }

    // ─────────────── HTML Builder ──────────────────────────────────────────

    private String buildEmailHtml(TransactionModel tx) {
        String riskLevel = tx.riskLevel == null ? "HIGH" : tx.riskLevel.toUpperCase();
        boolean isCritical = "CRITICAL".equals(riskLevel);

        String accentColor = isCritical ? "#ff0a47" : "#ff9100";
        String headerBg = isCritical ? "#fff5f7" : "#fff8f1";
        String sectionBg = isCritical ? "#fff9fb" : "#fffdf8";
        String actionBg = isCritical ? "#fff4f6" : "#eff6ff";
        String badgeHtml   = "<span style=\"background:" + accentColor
                + ";color:#fff;padding:6px 14px;font-weight:bold;font-size:13px;\">"
                + riskLevel + "</span>";

        String formattedAmount = "₹" + formatAmount(tx.amount);
        String formattedTime   = tx.timestamp == null ? "N/A"
                : tx.timestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a"));
        String mlPercent = String.format("%.1f%%", tx.mlFraudProbability * 100);

        // Risk score bar
        double score = tx.riskScore;
        int barPct   = (int) Math.min(score / 10.0 * 100, 100);
        String barColor;
        if (score >= 7.5)      barColor = "#ff0a47";
        else if (score >= 5.0) barColor = "#ff9100";
        else if (score >= 3.0) barColor = "#ffd740";
        else                   barColor = "#00e676";

        String scoreLabel = String.format("%.1f / 10.0 — %s", score, riskLevel);

        // IP risk tag color
        String ipColor = "TOR".equalsIgnoreCase(tx.ipRiskTag) || "PROXY".equalsIgnoreCase(tx.ipRiskTag)
                ? "#ff0a47" : "#00d4ff";

        // Risk level badge for the table
        String tableBadge = "<span style=\"background:" + accentColor
                + ";color:#fff;padding:4px 10px;font-weight:bold;font-size:12px;\">"
                + riskLevel + "</span>";

        // Triggered rules chips
        String rulesHtml = buildRulesHtml(tx.fraudReason);

        return """
                <html>
                <body style="margin:0;padding:0;background:#ffffff;color:#111827;font-family:Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ffffff;">
                <tr><td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;">

                <!-- 1. HEADER BAR -->
                <tr><td style="background:%s;padding:20px 32px;border-bottom:3px solid %s;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td style="font-size:22px;font-weight:bold;color:#111827;letter-spacing:2px;">FRAUDSHIELD</td>
                    <td align="right">%s</td>
                  </tr>
                  </table>
                </td></tr>

                <!-- 2. ALERT TITLE -->
                <tr><td style="background:%s;padding:24px 32px;border-top:1px solid #f3f4f6;">
                  <span style="font-size:32px;">⚠</span>
                  <div style="font-size:24px;color:#111827;font-weight:bold;margin-top:8px;">FRAUD TRANSACTION DETECTED</div>
                  <div style="font-size:13px;color:#6b7280;margin-top:4px;">Immediate review required</div>
                </td></tr>

                <!-- 3. TRANSACTION DETAILS -->
                <tr><td style="background:%s;padding:24px 32px;border-top:1px solid #f3f4f6;">
                  <div style="font-size:11px;letter-spacing:2px;color:#6b7280;text-transform:uppercase;margin-bottom:16px;">TRANSACTION DETAILS</div>
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                  </table>
                </td></tr>

                <!-- 4. TRIGGERED RULES -->
                <tr><td style="background:%s;padding:20px 32px;border-top:1px solid #f3f4f6;">
                  <div style="font-size:11px;letter-spacing:2px;color:#6b7280;text-transform:uppercase;margin-bottom:12px;">TRIGGERED FRAUD RULES</div>
                  %s
                </td></tr>

                <!-- 5. RISK SCORE BAR -->
                <tr><td style="background:%s;padding:20px 32px;border-top:1px solid #f3f4f6;">
                  <div style="font-size:11px;letter-spacing:2px;color:#6b7280;text-transform:uppercase;margin-bottom:10px;">RISK SCORE</div>
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#e5e7eb;height:8px;">
                  <tr><td style="width:%d%%;background:%s;height:8px;"></td><td></td></tr>
                  </table>
                  <div style="margin-top:8px;font-family:monospace;font-size:14px;color:%s;font-weight:bold;">%s</div>
                </td></tr>

                <!-- 6. ACTION FOOTER -->
                <tr><td style="background:%s;border-top:1px solid #e5e7eb;padding:20px 32px;">
                  <div style="color:#6b7280;font-size:13px;margin-bottom:12px;">Review this transaction in the dashboard</div>
                  <a href="http://localhost:8080/pages/dashboard.html"
                     style="display:inline-block;background:#1e7fd4;color:#ffffff;padding:10px 24px;font-weight:bold;text-decoration:none;font-size:13px;">OPEN FRAUDSHIELD DASHBOARD →</a>
                  <div style="margin-top:16px;font-size:11px;color:#9ca3af;">
                    This is an automated alert from FraudShield.<br>
                    Do not reply to this email.
                  </div>
                </td></tr>

                <!-- 7. FINAL FOOTER -->
                <tr><td style="background:#ffffff;padding:16px 32px;text-align:center;border-top:1px solid #f3f4f6;">
                  <div style="font-size:11px;color:#6b7280;">FraudShield v2.0 - Infosys Springboard Project</div>
                  <div style="font-size:10px;color:#9ca3af;margin-top:4px;">Sent by: %s</div>
                </td></tr>

                </table>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                headerBg,                                            // header bg
                accentColor,                                         // header border
                badgeHtml,                                           // header badge
                sectionBg,                                           // alert section bg
                sectionBg,                                           // details section bg
                sectionBg,                                           // rules section bg
                sectionBg,                                           // score section bg
                actionBg,                                            // action section bg
                row("Transaction ID", safe(tx.transactionId)),       // detail rows
                row("Account Holder", safe(tx.accountHolderName)),
                row("Amount", formattedAmount),
                row("Risk Score", String.format("%.1f / 10.0", score)),
                row("Risk Level", tableBadge),
                row("Location", safe(tx.location)),
                row("Merchant", safe(tx.merchantCategory)),
                row("Transaction Mode", safe(tx.transactionMode)),
                rowColored("IP Risk Tag", safe(tx.ipRiskTag), ipColor),
                row("ML Probability", mlPercent),
                row("Timestamp", formattedTime),
                rulesHtml,                                           // rules chips
                barPct, barColor,                                    // score bar
                barColor, scoreLabel,                                // score text
                senderAddress                                        // footer
        );
    }

    private String buildRoleAwareEmailHtml(AdminUser admin, TransactionModel tx) {
        String base = buildEmailHtml(tx);
        String note = admin.getRole() == UserRole.SUPERADMIN
                ? "You are receiving this alert as SUPERADMIN with full access to governance and remediation tools."
                : "You are receiving this alert as ADMIN for operational fraud response.";
        String roleTag = admin.getRole() == UserRole.SUPERADMIN ? "GOVERNANCE" : "OPERATIONS";
        String roleBg = admin.getRole() == UserRole.SUPERADMIN ? "#fff8e6" : "#eef6ff";
        String roleBlock = """
                <tr><td style=\"background:%s;border-top:1px solid #e5e7eb;padding:12px 32px;\">
                  <div style=\"font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#6b7280;\">Role Scope: %s</div>
                  <div style=\"font-size:12px;color:#374151;margin-top:4px;\">%s</div>
                </td></tr>
                """.formatted(roleBg, roleTag, note);
        return base.replace("<!-- 7. FINAL FOOTER -->", roleBlock + "\n\n                <!-- 7. FINAL FOOTER -->");
    }

    public String buildForgotPasswordEmailHtml(AdminUser admin, String temporaryPassword) {
        String role = admin.getRole() == null ? "USER" : admin.getRole().name();
        String accent = "SUPERADMIN".equals(role) ? "#b8860b" : "#1e7fd4";
        String scope = "SUPERADMIN".equals(role)
                ? "Governance-level account. Full platform access."
                : "Operational account for fraud monitoring and response.";
        String headerBg = "SUPERADMIN".equals(role) ? "#fff8e6" : "#eef6ff";
        String contentBg = "SUPERADMIN".equals(role) ? "#fffdf3" : "#f8fbff";
        String noticeBg = "SUPERADMIN".equals(role) ? "#fff3d4" : "#e8f2ff";

        return """
                <html>
                <body style=\"margin:0;padding:0;background:#ffffff;color:#1f2937;font-family:Arial,sans-serif;\">
                  <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;\">
                    <tr><td align=\"center\">
                      <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%%;\">
                        <tr><td style=\"background:%s;padding:20px 28px;border:1px solid #e5e7eb;border-bottom:3px solid %s;\">
                          <div style=\"font-size:22px;font-weight:bold;color:#111827;letter-spacing:1px;\">FraudShield</div>
                          <div style=\"font-size:12px;color:#6b7280;letter-spacing:0.5px;text-transform:uppercase;margin-top:6px;\">Password reset for %s</div>
                        </td></tr>
                        <tr><td style=\"background:%s;padding:24px 28px;border-left:1px solid #e5e7eb;border-right:1px solid #e5e7eb;\">
                          <p style=\"margin:0 0 10px 0;color:#111827;\">A password reset request was completed for your selected role.</p>
                          <p style=\"margin:0 0 16px 0;color:#6b7280;\">%s</p>
                          <div style=\"padding:12px 14px;background:#ffffff;border:1px solid %s;font-family:monospace;font-size:16px;color:#111827;letter-spacing:1px;\">%s</div>
                          <div style=\"margin:16px 0 0 0;color:#92400e;background:%s;padding:10px 12px;border:1px solid #fcd34d;\"><b>Action required:</b> Sign in with this temporary password and set a new password immediately.</div>
                        </td></tr>
                        <tr><td style=\"background:%s;padding:14px 28px;border:1px solid #e5e7eb;border-top:none;color:#6b7280;font-size:11px;\">This is an automated security email from FraudShield.</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(headerBg, accent, role, contentBg, scope, accent, temporaryPassword, noticeBg, contentBg);
    }

    private String row(String label, String value) {
        return "<tr>"
                + "<td style=\"padding:10px 0;border-bottom:1px solid #e5e7eb;font-size:11px;color:#6b7280;text-transform:uppercase;width:40%;\">" + label + "</td>"
                + "<td style=\"padding:10px 0;border-bottom:1px solid #e5e7eb;font-family:monospace;font-size:13px;color:#111827;\">" + value + "</td>"
                + "</tr>";
    }

    private String rowColored(String label, String value, String color) {
        return "<tr>"
                + "<td style=\"padding:10px 0;border-bottom:1px solid #e5e7eb;font-size:11px;color:#6b7280;text-transform:uppercase;width:40%;\">" + label + "</td>"
                + "<td style=\"padding:10px 0;border-bottom:1px solid #e5e7eb;font-family:monospace;font-size:13px;color:" + color + ";font-weight:bold;\">" + value + "</td>"
                + "</tr>";
    }

    private String buildRulesHtml(String fraudReason) {
        if (fraudReason == null || fraudReason.isBlank() || "None".equalsIgnoreCase(fraudReason)) {
            return "<span style=\"font-size:12px;color:#6b7280;\">No rules triggered</span>";
        }

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

        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">");
        String[] rules = fraudReason.split("\\s*\\|\\s*");
        for (String part : rules) {
            String trigger = part == null ? "" : part.trim();
            if (trigger.isEmpty()) {
                continue;
            }
            String code = trigger.contains(":") ? trigger.substring(0, trigger.indexOf(':')).trim() : trigger;
            String explanation = explanationMap.getOrDefault(code, "Fraud detector raised a custom signal for this transaction.");

            sb.append("<tr><td style=\"padding:10px 12px;border:1px solid #dbe5ef;background:#f8fbff;\">")
              .append("<div style=\"font-family:monospace;font-size:11px;color:#1e7fd4;font-weight:bold;\">")
              .append(code)
              .append("</div>")
              .append("<div style=\"font-family:monospace;font-size:11px;color:#374151;margin-top:4px;\">")
              .append(trigger)
              .append("</div>")
              .append("<div style=\"font-size:12px;color:#1f2937;margin-top:6px;\">")
              .append(explanation)
              .append("</div>")
              .append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }

    private String formatAmount(double amount) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("en", "IN"));
        return fmt.format(amount);
    }
}
