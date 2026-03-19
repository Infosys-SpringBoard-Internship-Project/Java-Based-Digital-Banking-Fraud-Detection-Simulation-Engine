package com.example.infosys_project.repository;

import com.example.infosys_project.model.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionModel, String> {
    // JpaRepository gives save(), findById(), findAll(), deleteById() for free
}
