package com.example.infosys_project.service;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.repository.AdminRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailAlertService {

    private static final Logger log = LoggerFactory.getLogger(EmailAlertService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AdminRepository adminRepository;

    @Value("${admin.alert.email}")
    private String adminEmail;

    @Async
    public void sendFraudAlert(TransactionModel tx) {
        try {
            String recipient = resolveAlertRecipient();
            if (recipient == null) {
                log.warn("Skipping fraud alert email because no alert recipient is configured");
                return;
            }

            String riskLevel = tx.riskLevel == null ? "HIGH" : tx.riskLevel.toUpperCase();
            String subjectPrefix = "CRITICAL".equals(riskLevel)
                    ? "⚠ [CRITICAL FRAUD ALERT]"
                    : "🔶 [HIGH RISK ALERT]";
            String subject = subjectPrefix + " ₹" + tx.amount + " — " + safe(tx.accountHolderName);

            String timestamp = tx.timestamp == null
                    ? "N/A"
                    : tx.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String htmlBody = """
                    <html>
                    <body style=\"margin:0;padding:20px;background:#0d1117;color:#c9d8e8;font-family:Arial,sans-serif;\">
                      <h2 style=\"margin-top:0;color:#ff3d57;\">FraudShield Alert</h2>
                      <p style=\"color:#c9d8e8;\">A suspicious transaction has been detected.</p>
                      <table style=\"border-collapse:collapse;width:100%;background:#111820;color:#c9d8e8;\" border=\"1\" cellpadding=\"8\" cellspacing=\"0\">
                        <tr><td><b>Transaction ID</b></td><td>%s</td></tr>
                        <tr><td><b>Name</b></td><td>%s</td></tr>
                        <tr><td><b>Amount</b></td><td>₹%s</td></tr>
                        <tr><td><b>Risk Score</b></td><td>%s</td></tr>
                        <tr><td><b>Risk Level</b></td><td>%s</td></tr>
                        <tr><td><b>Rules Triggered</b></td><td>%s</td></tr>
                        <tr><td><b>Location</b></td><td>%s</td></tr>
                        <tr><td><b>IP Risk Tag</b></td><td>%s</td></tr>
                        <tr><td><b>Merchant</b></td><td>%s</td></tr>
                        <tr><td><b>Timestamp</b></td><td>%s</td></tr>
                      </table>
                    </body>
                    </html>
                    """.formatted(
                    safe(tx.transactionId),
                    safe(tx.accountHolderName),
                    tx.amount,
                    tx.riskScore,
                    safe(tx.riskLevel),
                    safe(tx.fraudReason),
                    safe(tx.location),
                    safe(tx.ipRiskTag),
                        safe(tx.merchantId),
                    timestamp
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send fraud alert email: {}", e.getMessage(), e);
        }
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }

    private String resolveAlertRecipient() {
        if (adminEmail != null && !adminEmail.isBlank()) {
            return adminEmail.trim();
        }

        return adminRepository.findFirstByIsActiveTrueOrderByIdAsc()
                .map(AdminUser::getEmail)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .orElse(null);
    }
}
