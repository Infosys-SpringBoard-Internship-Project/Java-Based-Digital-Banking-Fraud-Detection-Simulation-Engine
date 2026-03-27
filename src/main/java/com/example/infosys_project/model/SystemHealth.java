package com.example.infosys_project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_health")
public class SystemHealth {

    @Id
    private Long id = 1L;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column(name = "db_status", length = 20)
    private String dbStatus;

    @Column(name = "ml_status", length = 20)
    private String mlStatus;

    @Column(name = "email_status", length = 20)
    private String emailStatus;

    @Column(name = "txn_processing_rate")
    private Double txnProcessingRate;

    @Column(name = "active_sessions")
    private Integer activeSessions;

    @Column(name = "error_count_1hr")
    private Integer errorCount1Hr;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = 1L;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
    public String getDbStatus() { return dbStatus; }
    public void setDbStatus(String dbStatus) { this.dbStatus = dbStatus; }
    public String getMlStatus() { return mlStatus; }
    public void setMlStatus(String mlStatus) { this.mlStatus = mlStatus; }
    public String getEmailStatus() { return emailStatus; }
    public void setEmailStatus(String emailStatus) { this.emailStatus = emailStatus; }
    public Double getTxnProcessingRate() { return txnProcessingRate; }
    public void setTxnProcessingRate(Double txnProcessingRate) { this.txnProcessingRate = txnProcessingRate; }
    public Integer getActiveSessions() { return activeSessions; }
    public void setActiveSessions(Integer activeSessions) { this.activeSessions = activeSessions; }
    public Integer getErrorCount1Hr() { return errorCount1Hr; }
    public void setErrorCount1Hr(Integer errorCount1Hr) { this.errorCount1Hr = errorCount1Hr; }
}
