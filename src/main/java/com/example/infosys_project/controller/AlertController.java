package com.example.infosys_project.controller;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.FraudAlert;
import com.example.infosys_project.repository.FraudAlertRepository;
import com.example.infosys_project.service.AuditService;
import com.example.infosys_project.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final FraudAlertRepository alertRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public AlertController(FraudAlertRepository alertRepository, AuthService authService, AuditService auditService) {
        this.alertRepository = alertRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<FraudAlert> getRecentAlerts() {
        return alertRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @GetMapping("/unread")
    public List<FraudAlert> getUnreadAlerts() {
        return alertRepository.findByIsReadFalse();
    }

    @GetMapping("/count")
    public Map<String, Long> getUnreadCount() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", alertRepository.countByIsReadFalse());
        return response;
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id,
                                         @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization,
                                         HttpServletRequest request) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setRead(true);
                    alertRepository.save(alert);
                    AdminUser actor = resolveActor(authorization);
                    if (actor != null) {
                        auditService.log(actor, "READ_ALERT", "ALERT", String.valueOf(id),
                                "Marked fraud alert as read", request.getRemoteAddr(), request.getHeader("User-Agent"));
                    }
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization,
                                            HttpServletRequest request) {
        List<FraudAlert> alerts = alertRepository.findAll();
        for (FraudAlert alert : alerts) {
            alert.setRead(true);
        }
        alertRepository.saveAll(alerts);
        AdminUser actor = resolveActor(authorization);
        if (actor != null) {
            auditService.log(actor, "READ_ALL_ALERTS", "ALERT", null,
                    "Marked all alerts as read", request.getRemoteAddr(), request.getHeader("User-Agent"));
        }
        return ResponseEntity.ok().build();
    }

    private AdminUser resolveActor(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authService.getAdminFromToken(authorization.substring(7).trim()).orElse(null);
    }
}
