package com.example.infosys_project.repository;

import com.example.infosys_project.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByOrderByTimestampDesc();
    List<AuditLog> findAllByOrderByTimestampDesc();
}
