package com.example.infosys_project.controller;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.AuditLog;
import com.example.infosys_project.repository.AuditLogRepository;
import com.example.infosys_project.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuthService authService;

    public AuditController(AuditLogRepository auditLogRepository, AuthService authService) {
        this.auditLogRepository = auditLogRepository;
        this.authService = authService;
    }

    @GetMapping("/logs")
    public List<AuditLog> getLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam(required = false) String user,
                                  @RequestParam(required = false) String action,
                                  @RequestParam(required = false) String date) {
        return filterLogs(resolveActor(authorization), user, action, date);
    }

    @GetMapping("/export-csv")
    public void exportCsv(@RequestHeader(value = "Authorization", required = false) String authorization,
                          @RequestParam(required = false) String user,
                          @RequestParam(required = false) String action,
                          @RequestParam(required = false) String date,
                          HttpServletResponse response) throws IOException {
        List<AuditLog> logs = filterLogs(resolveActor(authorization), user, action, date);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");
        PrintWriter w = response.getWriter();
        w.println("timestamp,user_email,user_role,action_type,target_entity,target_id,details");
        for (AuditLog log : logs) {
            w.println(csv(log.getTimestamp() == null ? null : log.getTimestamp().toString()) + ","
                    + csv(log.getUserEmail()) + ","
                    + csv(log.getUserRole()) + ","
                    + csv(log.getActionType()) + ","
                    + csv(log.getTargetEntity()) + ","
                    + csv(log.getTargetId()) + ","
                    + csv(log.getDetails()));
        }
        w.flush();
    }

    private List<AuditLog> filterLogs(AdminUser actor, String user, String action, String date) {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        if (actor != null && actor.getRole() == com.example.infosys_project.model.UserRole.ADMIN) {
            logs = logs.stream()
                    .filter(log -> !"SUPERADMIN".equalsIgnoreCase(log.getUserRole()))
                    .collect(Collectors.toList());
        }

        if (user != null && !user.isBlank()) {
            String needle = user.trim().toLowerCase();
            logs = logs.stream().filter(log -> log.getUserEmail() != null && log.getUserEmail().toLowerCase().contains(needle)).collect(Collectors.toList());
        }
        if (action != null && !action.isBlank()) {
            String needle = action.trim().toLowerCase();
            logs = logs.stream().filter(log -> log.getActionType() != null && log.getActionType().toLowerCase().contains(needle)).collect(Collectors.toList());
        }
        if (date != null && !date.isBlank()) {
            try {
                LocalDate targetDate = LocalDate.parse(date.trim());
                logs = logs.stream().filter(log -> log.getTimestamp() != null && log.getTimestamp().toLocalDate().equals(targetDate)).collect(Collectors.toList());
            } catch (DateTimeParseException ignored) {
            }
        }
        return logs.stream().limit(200).collect(Collectors.toList());
    }

    private AdminUser resolveActor(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authService.getAdminFromToken(authorization.substring(7).trim()).orElse(null);
    }

    private String csv(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
