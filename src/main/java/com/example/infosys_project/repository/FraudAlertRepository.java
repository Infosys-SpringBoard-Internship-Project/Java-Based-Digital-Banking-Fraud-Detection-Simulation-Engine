package com.example.infosys_project.repository;

import com.example.infosys_project.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByIsReadFalse();
    List<FraudAlert> findTop20ByOrderByCreatedAtDesc();
    long countByIsReadFalse();
}
