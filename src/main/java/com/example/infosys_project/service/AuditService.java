package com.example.infosys_project.service;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.AuditLog;
import com.example.infosys_project.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(AdminUser actor,
                    String actionType,
                    String targetEntity,
                    String targetId,
                    String details,
                    String ipAddress,
                    String userAgent) {
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setUserEmail(actor != null ? actor.getEmail() : "ANONYMOUS");
        log.setUserRole(actor != null && actor.getRole() != null ? actor.getRole().name() : "UNKNOWN");
        log.setActionType(actionType);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        auditLogRepository.save(log);
    }
}
