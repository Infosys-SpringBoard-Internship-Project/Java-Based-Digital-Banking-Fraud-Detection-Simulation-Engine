package com.example.infosys_project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fraud_alerts",
        indexes = {
                @Index(name = "idx_fraud_alerts_created_at", columnList = "created_at"),
                @Index(name = "idx_fraud_alerts_is_read", columnList = "is_read"),
                @Index(name = "idx_fraud_alerts_transaction_id", columnList = "transaction_id")
        }
)
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    private String accountHolderName;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "risk_score", nullable = false)
    private double riskScore;

    @Column(name = "fraud_reason", length = 1000)
    private String fraudReason;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "ip_risk_tag", length = 50)
    private String ipRiskTag;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    public FraudAlert() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getFraudReason() {
        return fraudReason;
    }

    public void setFraudReason(String fraudReason) {
        this.fraudReason = fraudReason;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIpRiskTag() {
        return ipRiskTag;
    }

    public void setIpRiskTag(String ipRiskTag) {
        this.ipRiskTag = ipRiskTag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
