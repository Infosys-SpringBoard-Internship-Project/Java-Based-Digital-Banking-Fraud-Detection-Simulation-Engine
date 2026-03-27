package com.example.infosys_project.repository;

import com.example.infosys_project.model.SystemHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemHealthRepository extends JpaRepository<SystemHealth, Long> {
}
