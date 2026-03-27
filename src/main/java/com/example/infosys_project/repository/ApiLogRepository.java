package com.example.infosys_project.repository;

import com.example.infosys_project.model.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {
    List<ApiLog> findTop100ByOrderByTimestampDesc();

    @Query("select count(a) from ApiLog a where a.timestamp >= :since and a.statusCode >= 500")
    long countErrorsSince(LocalDateTime since);

    @Query("select count(a) from ApiLog a where a.timestamp >= :since")
    long countSince(LocalDateTime since);
}
