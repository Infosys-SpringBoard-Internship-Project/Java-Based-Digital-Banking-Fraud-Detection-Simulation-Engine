package com.example.infosys_project.controller;

import com.example.infosys_project.model.FraudAlert;
import com.example.infosys_project.repository.FraudAlertRepository;
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

    public AlertController(FraudAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
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
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setRead(true);
                    alertRepository.save(alert);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        List<FraudAlert> alerts = alertRepository.findAll();
        for (FraudAlert alert : alerts) {
            alert.setRead(true);
        }
        alertRepository.saveAll(alerts);
        return ResponseEntity.ok().build();
    }
}
