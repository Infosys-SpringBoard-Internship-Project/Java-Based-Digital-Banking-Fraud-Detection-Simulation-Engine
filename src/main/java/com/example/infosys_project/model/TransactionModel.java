package com.example.infosys_project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_timestamp", columnList = "timestamp"),
                @Index(name = "idx_transactions_risk_level", columnList = "risk_level"),
                @Index(name = "idx_transactions_is_fraud", columnList = "is_fraud"),
                @Index(name = "idx_transactions_utr_number", columnList = "utr_number", unique = true)
        }
)
public class TransactionModel {

    @Id
    @Column(name = "transaction_id", nullable = false, length = 36)
    public String transactionId;

    @Column(name = "utr_number", nullable = false, length = 32)
    public String utrNumber;

    @Column(name = "timestamp", nullable = false)
    public LocalDateTime timestamp;

    // Account information.
    @Column(name = "account_holder_name", nullable = false, length = 255)
    public String accountHolderName;

    @Column(name = "mobile_number", nullable = false, length = 20)
    public String mobileNumber;

    @Column(name = "sender_account", nullable = false, length = 34)
    public String senderAccount;

    @Column(name = "receiver_account", nullable = false, length = 34)
    public String receiverAccount;

    @Column(name = "bank_name", nullable = false, length = 120)
    public String bankName;

    @Column(name = "account_age_days", nullable = false)
    public int accountAgeDays;

    // Transaction details.
    @Column(name = "type", nullable = false, length = 50)
    public String type;

    @Column(name = "amount", nullable = false)
    public double amount;

    @Column(name = "balance", nullable = false)
    public double balance;

    @Column(name = "currency", nullable = false, length = 10)
    public String currency;

    @Column(name = "merchant_category", length = 120)
    public String merchantCategory;

    @Column(name = "merchant_id", length = 120)
    public String merchantId;

    @Column(name = "transaction_mode", length = 50)
    public String transactionMode;

    // Location and device context.
    @Column(name = "location", length = 255)
    public String location;

    @Column(name = "previous_location", length = 255)
    public String previousLocation;

    @Column(name = "is_new_location", nullable = false)
    public boolean isNewLocation;

    @Column(name = "distance_from_last_txn_km", nullable = false)
    public double distanceFromLastTxnKm;

    @Column(name = "device", length = 120)
    public String device;

    @Column(name = "is_new_device", nullable = false)
    public boolean isNewDevice;

    @Column(name = "ip_address", length = 64)
    public String ipAddress;

    // IP intelligence fields.
    @Column(name = "is_vpn_or_proxy", nullable = false)
    public boolean isVpnOrProxy;

    @Column(name = "ip_country", length = 120)
    public String ipCountry;

    @Column(name = "ip_matches_location", nullable = false)
    public boolean ipMatchesLocation;

    @Column(name = "ip_risk_tag", nullable = false, length = 50)
    public String ipRiskTag;   // CLEAN | VPN | PROXY | TOR | DATACENTER

    // Behavioral fields.
    @Column(name = "txn_count_last_1hr", nullable = false)
    public int txnCountLastHour;

    @Column(name = "txn_count_last_24hr", nullable = false)
    public int txnCountLast24Hours;

    @Column(name = "avg_txn_amount_30days", nullable = false)
    public double avgTxnAmount30Days;

    @Column(name = "is_international", nullable = false)
    public boolean isInternational;

    @Column(name = "is_first_time_receiver", nullable = false)
    public boolean isFirstTimeReceiver;

    // Fraud result fields.
    @Column(name = "is_fraud", nullable = false)
    public boolean isFraud;

    @Column(name = "fraud_reason", length = 1000)
    public String fraudReason;

    @Column(name = "risk_score", nullable = false)
    public double riskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    public String riskLevel;   // NORMAL | MEDIUM | HIGH | CRITICAL

    @Column(name = "ml_fraud_probability", nullable = false)
    public double mlFraudProbability;

    // Required by JPA.
    public TransactionModel() {}

    public TransactionModel(
            String accountHolderName, String mobileNumber,
            String senderAccount,     String receiverAccount,
            String bankName,          int accountAgeDays,
            String type,              double amount,         double balance,
            String currency,          String merchantCategory,
            String merchantId,        String transactionMode,
            String location,          String previousLocation,
            boolean isNewLocation,    double distanceFromLastTxnKm,
            String device,            boolean isNewDevice,
            String ipAddress,         boolean isVpnOrProxy,
            String ipCountry,         boolean ipMatchesLocation,  String ipRiskTag,
            int txnCountLastHour,     int txnCountLast24Hours,
            double avgTxnAmount30Days,
            boolean isInternational,  boolean isFirstTimeReceiver
    ) {
        this.transactionId         = UUID.randomUUID().toString();
        this.utrNumber             = "UTR" + System.currentTimeMillis();
        this.timestamp             = LocalDateTime.now();
        this.accountHolderName     = accountHolderName;
        this.mobileNumber          = mobileNumber;
        this.senderAccount         = senderAccount;
        this.receiverAccount       = receiverAccount;
        this.bankName              = bankName;
        this.accountAgeDays        = accountAgeDays;
        this.type                  = type;
        this.amount                = Math.round(amount * 100.0) / 100.0;
        this.balance               = balance;
        this.currency              = currency;
        this.merchantCategory      = merchantCategory;
        this.merchantId            = merchantId;
        this.transactionMode       = transactionMode;
        this.location              = location;
        this.previousLocation      = previousLocation;
        this.isNewLocation         = isNewLocation;
        this.distanceFromLastTxnKm = distanceFromLastTxnKm;
        this.device                = device;
        this.isNewDevice           = isNewDevice;
        this.ipAddress             = ipAddress;
        this.isVpnOrProxy          = isVpnOrProxy;
        this.ipCountry             = ipCountry;
        this.ipMatchesLocation     = ipMatchesLocation;
        this.ipRiskTag             = ipRiskTag;
        this.txnCountLastHour      = txnCountLastHour;
        this.txnCountLast24Hours   = txnCountLast24Hours;
        this.avgTxnAmount30Days    = avgTxnAmount30Days;
        this.isInternational       = isInternational;
        this.isFirstTimeReceiver   = isFirstTimeReceiver;
        this.isFraud               = false;
        this.fraudReason           = "None";
        this.riskScore             = 0.0;
        this.riskLevel             = "NORMAL";
        this.mlFraudProbability    = 0.0;
    }
}
