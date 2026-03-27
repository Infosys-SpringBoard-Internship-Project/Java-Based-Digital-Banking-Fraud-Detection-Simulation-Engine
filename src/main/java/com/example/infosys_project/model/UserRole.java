package com.example.infosys_project.model;

/**
 * User role hierarchy for FraudShield
 * SUPERADMIN > ADMIN > ANALYST
 */
public enum UserRole {
    /**
     * SUPERADMIN - Initial account only
     * - Can create ADMIN and ANALYST users
     * - Full system access
     * - Cannot be deleted
     */
    SUPERADMIN,
    
    /**
     * ADMIN - Full access administrators
     * - Can create ANALYST users
     * - Full access (view, create, edit, delete)
     * - Can opt-in/out of email alerts
     */
    ADMIN,
    
    /**
     * ANALYST - Read-only users
     * - Cannot create users
     * - Read-only access to all data
     * - Can export data (CSV)
     */
    ANALYST
}
