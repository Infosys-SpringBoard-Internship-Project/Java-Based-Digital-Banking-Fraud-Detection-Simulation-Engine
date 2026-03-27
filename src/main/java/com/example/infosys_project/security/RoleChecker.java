package com.example.infosys_project.security;

import com.example.infosys_project.model.UserRole;

/**
 * Utility class for checking role-based permissions
 * Implements the permission hierarchy for FraudShield
 */
public class RoleChecker {

    /**
     * Check if the given role can create an ADMIN user
     * Only SUPERADMIN can create ADMIN users
     */
    public static boolean canCreateAdmin(UserRole role) {
        return role == UserRole.SUPERADMIN;
    }

    /**
     * Check if the given role can create an ANALYST user
     * SUPERADMIN and ADMIN can create ANALYST users
     */
    public static boolean canCreateAnalyst(UserRole role) {
        return role == UserRole.SUPERADMIN || role == UserRole.ADMIN;
    }

    /**
     * Check if the given role has write access
     * SUPERADMIN and ADMIN have write access
     * ANALYST has read-only access
     */
    public static boolean hasWriteAccess(UserRole role) {
        return role == UserRole.SUPERADMIN || role == UserRole.ADMIN;
    }

    /**
     * Check if the given role can export data
     * All roles can export data
     */
    public static boolean canExportData(UserRole role) {
        return true; // All roles can export
    }

    /**
     * Check if the given role can manage users (view user list)
     * SUPERADMIN and ADMIN can manage users
     */
    public static boolean canManageUsers(UserRole role) {
        return role == UserRole.SUPERADMIN || role == UserRole.ADMIN;
    }

    /**
     * Check if the given role can delete a specific user
     * SUPERADMIN can delete anyone except themselves
     * ADMIN can delete ANALYST users only
     */
    public static boolean canDeleteUser(UserRole deleterRole, UserRole targetRole, Boolean targetCanBeDeleted) {
        if (targetCanBeDeleted != null && !targetCanBeDeleted) {
            return false; // Cannot delete SUPERADMIN
        }
        
        if (deleterRole == UserRole.SUPERADMIN) {
            return targetCanBeDeleted != null && targetCanBeDeleted;
        }
        
        if (deleterRole == UserRole.ADMIN) {
            return targetRole == UserRole.ANALYST;
        }
        
        return false;
    }

    /**
     * Check if the given role can update a specific user
     * SUPERADMIN can update anyone
     * ADMIN can update ANALYST users and themselves
     */
    public static boolean canUpdateUser(UserRole updaterRole, UserRole targetRole, Long updaterId, Long targetId) {
        if (updaterRole == UserRole.SUPERADMIN) {
            return true;
        }
        
        if (updaterRole == UserRole.ADMIN) {
            return targetRole == UserRole.ANALYST || updaterId.equals(targetId);
        }
        
        return false;
    }

    /**
     * Check if the given role can access admin-only pages
     * SUPERADMIN and ADMIN have access
     */
    public static boolean canAccessAdminPages(UserRole role) {
        return role == UserRole.SUPERADMIN || role == UserRole.ADMIN;
    }

    /**
     * Check if the given role can access the simulation page.
     * All authenticated roles can open simulation telemetry,
     * while write actions are still governed by API write permissions.
     */
    public static boolean canAccessSimulation(UserRole role) {
        return role == UserRole.SUPERADMIN || role == UserRole.ADMIN || role == UserRole.ANALYST;
    }

    /**
     * Check if the given role can create a user with the target role
     */
    public static boolean canCreateUserWithRole(UserRole creatorRole, UserRole targetRole) {
        if (targetRole == UserRole.SUPERADMIN) {
            return false; // Cannot create SUPERADMIN
        }
        
        if (targetRole == UserRole.ADMIN) {
            return canCreateAdmin(creatorRole);
        }
        
        if (targetRole == UserRole.ANALYST) {
            return canCreateAnalyst(creatorRole);
        }
        
        return false;
    }
}
