package com.example.infosys_project.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_logs", indexes = {
        @Index(name = "idx_api_logs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_api_logs_endpoint", columnList = "endpoint"),
        @Index(name = "idx_api_logs_status_code", columnList = "status_code")
})
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
