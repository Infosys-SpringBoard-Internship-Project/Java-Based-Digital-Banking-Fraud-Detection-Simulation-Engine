package com.example.infosys_project.dto;

import com.example.infosys_project.model.TransactionModel;

public class ValidationResponse {
    public TransactionModel transaction;   // full transaction object
    public String validationResult;        // raw validator message
    public String status;                  // CLEAN | REVIEW_NEEDED | FRAUD_DETECTED | REJECTED
    public boolean saved;                  // true if written to DB
    public String message;                 // human-readable summary
}
