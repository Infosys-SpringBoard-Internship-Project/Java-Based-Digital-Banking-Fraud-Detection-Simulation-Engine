package com.example.infosys_project.repository;

import com.example.infosys_project.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByEmail(String email);
    List<AdminUser> findByIsActiveTrue();
    List<AdminUser> findByIsActiveTrueAndEmailAlertsEnabledTrue();
    Optional<AdminUser> findFirstByIsActiveTrueAndLastLoginIsNotNullOrderByLastLoginDescIdDesc();
    Optional<AdminUser> findFirstByIsActiveTrueOrderByIdAsc();
    Optional<AdminUser> findFirstByIsActiveTrueOrderByCreatedAtDescIdDesc();
}
