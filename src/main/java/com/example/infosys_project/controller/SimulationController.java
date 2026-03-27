package com.example.infosys_project.controller;

import com.example.infosys_project.dto.SimulationRequest;
import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.service.AuditService;
import com.example.infosys_project.service.AuthService;
import com.example.infosys_project.service.SimulationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationService simulationService;
    private final AuthService authService;
    private final AuditService auditService;

    public SimulationController(SimulationService simulationService, AuthService authService, AuditService auditService) {
        this.simulationService = simulationService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody(required = false) SimulationRequest request,
                                   @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization,
                                   HttpServletRequest httpRequest) {
        try {
            AdminUser actor = resolveActor(authorization);
            ResponseEntity<?> response = ResponseEntity.ok(simulationService.startSimulation(request));
            if (actor != null) {
                auditService.log(actor, "START_SIMULATION", "SIMULATION", null,
                        "Started simulation run", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            }
            return response;
        } catch (RuntimeException ex) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest httpRequest) {
        AdminUser actor = resolveActor(authorization);
        if (actor != null) {
            auditService.log(actor, "STOP_SIMULATION", "SIMULATION", null,
                    "Stop requested for simulation run", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        }
        return ResponseEntity.ok(simulationService.stopSimulation());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return simulationService.getStatus();
    }

    private AdminUser resolveActor(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authService.getAdminFromToken(authorization.substring(7).trim()).orElse(null);
    }
}
