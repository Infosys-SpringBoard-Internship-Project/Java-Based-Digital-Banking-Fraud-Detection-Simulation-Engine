package com.example.infosys_project.security;

import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.model.UserRole;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for masking sensitive data in transaction records.
 * ANALYST role users should see masked data for PII fields.
 *
 * Masked Fields:
 * - accountHolderName: "John Doe" -> "J*** D**"
 * - mobileNumber: "9876543210" -> "98****3210"
 * - senderAccount: "123456789012" -> "1234****9012"
 * - receiverAccount: "987654321098" -> "9876****1098"
 * - ipAddress: "192.168.1.100" -> "192.***.***.***"
 */
public class DataMaskingUtil {

    private DataMaskingUtil() {
        // Utility class, no instantiation
    }

    /**
     * Check if data masking should be applied for the given role.
     * Only ANALYST role requires data masking.
     */
    public static boolean shouldMaskData(UserRole role) {
        return role == UserRole.ANALYST;
    }

    /**
     * Mask a single transaction model.
     * Creates a copy with sensitive fields masked.
     */
    public static TransactionModel maskTransaction(TransactionModel original) {
        if (original == null) {
            return null;
        }

        // Create a shallow copy with masked fields
        TransactionModel masked = new TransactionModel();

        // Copy all fields
        masked.transactionId = original.transactionId;
        masked.utrNumber = original.utrNumber;
        masked.timestamp = original.timestamp;
        masked.accountHolderName = maskName(original.accountHolderName);
        masked.mobileNumber = maskMobileNumber(original.mobileNumber);
        masked.senderAccount = maskAccountNumber(original.senderAccount);
        masked.receiverAccount = maskAccountNumber(original.receiverAccount);
        masked.bankName = original.bankName;
        masked.accountAgeDays = original.accountAgeDays;
        masked.type = original.type;
        masked.amount = original.amount;
        masked.balance = original.balance;
        masked.currency = original.currency;
        masked.merchantCategory = original.merchantCategory;
        masked.merchantId = original.merchantId;
        masked.transactionMode = original.transactionMode;
        masked.location = original.location;
        masked.previousLocation = original.previousLocation;
        masked.isNewLocation = original.isNewLocation;
        masked.distanceFromLastTxnKm = original.distanceFromLastTxnKm;
        masked.device = original.device;
        masked.isNewDevice = original.isNewDevice;
        masked.ipAddress = maskIpAddress(original.ipAddress);
        masked.isVpnOrProxy = original.isVpnOrProxy;
        masked.ipCountry = original.ipCountry;
        masked.ipMatchesLocation = original.ipMatchesLocation;
        masked.ipRiskTag = original.ipRiskTag;
        masked.txnCountLastHour = original.txnCountLastHour;
        masked.txnCountLast24Hours = original.txnCountLast24Hours;
        masked.avgTxnAmount30Days = original.avgTxnAmount30Days;
        masked.isInternational = original.isInternational;
        masked.isFirstTimeReceiver = original.isFirstTimeReceiver;
        masked.isFraud = original.isFraud;
        masked.fraudReason = original.fraudReason;
        masked.riskScore = original.riskScore;
        masked.riskLevel = original.riskLevel;
        masked.mlFraudProbability = original.mlFraudProbability;

        return masked;
    }

    /**
     * Mask a list of transactions.
     */
    public static List<TransactionModel> maskTransactions(List<TransactionModel> transactions) {
        if (transactions == null) {
            return null;
        }
        return transactions.stream()
                .map(DataMaskingUtil::maskTransaction)
                .collect(Collectors.toList());
    }

    /**
     * Mask a name: "John Doe" -> "J*** D**"
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String[] parts = name.split("\\s+");
        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                masked.append(" ");
            }
            String part = parts[i];
            if (part.length() > 0) {
                masked.append(part.charAt(0));
                masked.append("*".repeat(Math.max(0, part.length() - 1)));
            }
        }

        return masked.toString();
    }

    /**
     * Mask mobile number: "9876543210" -> "98****3210"
     */
    public static String maskMobileNumber(String mobile) {
        if (mobile == null || mobile.length() < 6) {
            return mobile;
        }

        int len = mobile.length();
        String prefix = mobile.substring(0, 2);
        String suffix = mobile.substring(len - 4);
        String masked = "*".repeat(len - 6);

        return prefix + masked + suffix;
    }

    /**
     * Mask account number: "123456789012" -> "1234****9012"
     */
    public static String maskAccountNumber(String account) {
        if (account == null || account.length() < 8) {
            return account;
        }

        int len = account.length();
        String prefix = account.substring(0, 4);
        String suffix = account.substring(len - 4);
        String masked = "*".repeat(len - 8);

        return prefix + masked + suffix;
    }

    /**
     * Mask IP address: "192.168.1.100" -> "192.***.***.***"
     */
    public static String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        // Handle IPv4
        if (ip.contains(".")) {
            String[] octets = ip.split("\\.");
            if (octets.length >= 2) {
                return octets[0] + "." + octets[1] + ".***.***";
            }
        }

        // Handle IPv6 - mask everything except first block
        if (ip.contains(":")) {
            String[] blocks = ip.split(":");
            if (blocks.length >= 1) {
                return blocks[0] + ":****:****:****:****:****:****:****";
            }
        }

        // Unknown format, return masked
        return "***.***.***";
    }
}
