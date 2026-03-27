package com.example.infosys_project.controller;

import com.example.infosys_project.model.ApiLog;
import com.example.infosys_project.repository.ApiLogRepository;
import com.example.infosys_project.service.AutoTransactionGenerationService;
import com.example.infosys_project.service.HealthMonitorService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
@CrossOrigin(origins = "*")
public class SystemController {

    private final HealthMonitorService healthMonitorService;
    private final ApiLogRepository apiLogRepository;
    private final AutoTransactionGenerationService autoTransactionGenerationService;

    public SystemController(HealthMonitorService healthMonitorService,
                            ApiLogRepository apiLogRepository,
                            AutoTransactionGenerationService autoTransactionGenerationService) {
        this.healthMonitorService = healthMonitorService;
        this.apiLogRepository = apiLogRepository;
        this.autoTransactionGenerationService = autoTransactionGenerationService;
    }

    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        return healthMonitorService.buildSystemOverview();
    }

    @GetMapping("/api-logs")
    public List<ApiLog> getApiLogs() {
        return apiLogRepository.findTop100ByOrderByTimestampDesc();
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("health", healthMonitorService.buildSystemOverview());
        response.put("recentLogs", apiLogRepository.findTop100ByOrderByTimestampDesc());
        response.put("autoGeneration", autoTransactionGenerationService.getStatus());
        return response;
    }

    @GetMapping("/auto-generation")
    public Map<String, Object> getAutoGenerationStatus() {
        return autoTransactionGenerationService.getStatus();
    }

    @PostMapping("/auto-generation/start")
    public Map<String, Object> startAutoGeneration() {
        return autoTransactionGenerationService.start();
    }

    @PostMapping("/auto-generation/stop")
    public Map<String, Object> stopAutoGeneration() {
        return autoTransactionGenerationService.stop();
    }
}
