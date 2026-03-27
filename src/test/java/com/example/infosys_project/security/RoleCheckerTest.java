package com.example.infosys_project.security;

import com.example.infosys_project.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for RoleChecker
 * Tests all role permission logic according to Phase 8 requirements
 */
@DisplayName("RoleChecker Permission Tests")
class RoleCheckerTest {

    @Nested
    @DisplayName("canCreateAdmin() tests")
    class CanCreateAdminTests {
        
        @Test
        @DisplayName("SUPERADMIN can create ADMIN")
        void superadminCanCreateAdmin() {
            assertTrue(RoleChecker.canCreateAdmin(UserRole.SUPERADMIN),
                "SUPERADMIN should be able to create ADMIN users");
        }
        
        @Test
        @DisplayName("ADMIN cannot create ADMIN")
        void adminCannotCreateAdmin() {
            assertFalse(RoleChecker.canCreateAdmin(UserRole.ADMIN),
                "ADMIN should not be able to create ADMIN users");
        }
        
        @Test
        @DisplayName("ANALYST cannot create ADMIN")
        void analystCannotCreateAdmin() {
            assertFalse(RoleChecker.canCreateAdmin(UserRole.ANALYST),
                "ANALYST should not be able to create ADMIN users");
        }
    }

    @Nested
    @DisplayName("canCreateAnalyst() tests")
    class CanCreateAnalystTests {
        
        @Test
        @DisplayName("SUPERADMIN can create ANALYST")
        void superadminCanCreateAnalyst() {
            assertTrue(RoleChecker.canCreateAnalyst(UserRole.SUPERADMIN),
                "SUPERADMIN should be able to create ANALYST users");
        }
        
        @Test
        @DisplayName("ADMIN can create ANALYST")
        void adminCanCreateAnalyst() {
            assertTrue(RoleChecker.canCreateAnalyst(UserRole.ADMIN),
                "ADMIN should be able to create ANALYST users");
        }
        
        @Test
        @DisplayName("ANALYST cannot create ANALYST")
        void analystCannotCreateAnalyst() {
            assertFalse(RoleChecker.canCreateAnalyst(UserRole.ANALYST),
                "ANALYST should not be able to create ANALYST users");
        }
    }

    @Nested
    @DisplayName("hasWriteAccess() tests")
    class HasWriteAccessTests {
        
        @Test
        @DisplayName("SUPERADMIN has write access")
        void superadminHasWriteAccess() {
            assertTrue(RoleChecker.hasWriteAccess(UserRole.SUPERADMIN),
                "SUPERADMIN should have write access");
        }
        
        @Test
        @DisplayName("ADMIN has write access")
        void adminHasWriteAccess() {
            assertTrue(RoleChecker.hasWriteAccess(UserRole.ADMIN),
                "ADMIN should have write access");
        }
        
        @Test
        @DisplayName("ANALYST does not have write access")
        void analystDoesNotHaveWriteAccess() {
            assertFalse(RoleChecker.hasWriteAccess(UserRole.ANALYST),
                "ANALYST should only have read-only access");
        }
    }

    @Nested
    @DisplayName("canExportData() tests")
    class CanExportDataTests {
        
        @ParameterizedTest
        @EnumSource(UserRole.class)
        @DisplayName("All roles can export data")
        void allRolesCanExportData(UserRole role) {
            assertTrue(RoleChecker.canExportData(role),
                role + " should be able to export data");
        }
    }

    @Nested
    @DisplayName("canManageUsers() tests")
    class CanManageUsersTests {
        
        @Test
        @DisplayName("SUPERADMIN can manage users")
        void superadminCanManageUsers() {
            assertTrue(RoleChecker.canManageUsers(UserRole.SUPERADMIN),
                "SUPERADMIN should be able to manage users");
        }
        
        @Test
        @DisplayName("ADMIN can manage users")
        void adminCanManageUsers() {
            assertTrue(RoleChecker.canManageUsers(UserRole.ADMIN),
                "ADMIN should be able to manage users");
        }
        
        @Test
        @DisplayName("ANALYST cannot manage users")
        void analystCannotManageUsers() {
            assertFalse(RoleChecker.canManageUsers(UserRole.ANALYST),
                "ANALYST should not be able to manage users");
        }
    }

    @Nested
    @DisplayName("canDeleteUser() tests")
    class CanDeleteUserTests {
        
        @Test
        @DisplayName("SUPERADMIN cannot delete themselves")
        void superadminCannotDeleteThemselves() {
            assertFalse(RoleChecker.canDeleteUser(UserRole.SUPERADMIN, UserRole.SUPERADMIN, false),
                "SUPERADMIN should not be able to delete themselves");
        }
        
        @Test
        @DisplayName("SUPERADMIN can delete ADMIN")
        void superadminCanDeleteAdmin() {
            assertTrue(RoleChecker.canDeleteUser(UserRole.SUPERADMIN, UserRole.ADMIN, true),
                "SUPERADMIN should be able to delete ADMIN users");
        }
        
        @Test
        @DisplayName("SUPERADMIN can delete ANALYST")
        void superadminCanDeleteAnalyst() {
            assertTrue(RoleChecker.canDeleteUser(UserRole.SUPERADMIN, UserRole.ANALYST, true),
                "SUPERADMIN should be able to delete ANALYST users");
        }
        
        @Test
        @DisplayName("ADMIN cannot delete ADMIN")
        void adminCannotDeleteAdmin() {
            assertFalse(RoleChecker.canDeleteUser(UserRole.ADMIN, UserRole.ADMIN, true),
                "ADMIN should not be able to delete other ADMIN users");
        }
        
        @Test
        @DisplayName("ADMIN can delete ANALYST")
        void adminCanDeleteAnalyst() {
            assertTrue(RoleChecker.canDeleteUser(UserRole.ADMIN, UserRole.ANALYST, true),
                "ADMIN should be able to delete ANALYST users");
        }
        
        @Test
        @DisplayName("ANALYST cannot delete anyone")
        void analystCannotDeleteAnyone() {
            assertFalse(RoleChecker.canDeleteUser(UserRole.ANALYST, UserRole.ANALYST, true),
                "ANALYST should not be able to delete any users");
            assertFalse(RoleChecker.canDeleteUser(UserRole.ANALYST, UserRole.ADMIN, true),
                "ANALYST should not be able to delete ADMIN users");
            assertFalse(RoleChecker.canDeleteUser(UserRole.ANALYST, UserRole.SUPERADMIN, false),
                "ANALYST should not be able to delete SUPERADMIN");
        }
    }

    @Nested
    @DisplayName("canUpdateUser() tests")
    class CanUpdateUserTests {
        
        @Test
        @DisplayName("SUPERADMIN can update anyone")
        void superadminCanUpdateAnyone() {
            assertTrue(RoleChecker.canUpdateUser(UserRole.SUPERADMIN, UserRole.SUPERADMIN, 1L, 2L),
                "SUPERADMIN should be able to update any user");
            assertTrue(RoleChecker.canUpdateUser(UserRole.SUPERADMIN, UserRole.ADMIN, 1L, 2L),
                "SUPERADMIN should be able to update ADMIN users");
            assertTrue(RoleChecker.canUpdateUser(UserRole.SUPERADMIN, UserRole.ANALYST, 1L, 2L),
                "SUPERADMIN should be able to update ANALYST users");
        }
        
        @Test
        @DisplayName("ADMIN can update ANALYST")
        void adminCanUpdateAnalyst() {
            assertTrue(RoleChecker.canUpdateUser(UserRole.ADMIN, UserRole.ANALYST, 1L, 2L),
                "ADMIN should be able to update ANALYST users");
        }
        
        @Test
        @DisplayName("ADMIN can update themselves")
        void adminCanUpdateThemselves() {
            assertTrue(RoleChecker.canUpdateUser(UserRole.ADMIN, UserRole.ADMIN, 1L, 1L),
                "ADMIN should be able to update themselves");
        }
        
        @Test
        @DisplayName("ADMIN cannot update other ADMIN")
        void adminCannotUpdateOtherAdmin() {
            assertFalse(RoleChecker.canUpdateUser(UserRole.ADMIN, UserRole.ADMIN, 1L, 2L),
                "ADMIN should not be able to update other ADMIN users");
        }
        
        @Test
        @DisplayName("ANALYST cannot update anyone")
        void analystCannotUpdateAnyone() {
            assertFalse(RoleChecker.canUpdateUser(UserRole.ANALYST, UserRole.ANALYST, 1L, 2L),
                "ANALYST should not be able to update users");
            assertFalse(RoleChecker.canUpdateUser(UserRole.ANALYST, UserRole.ADMIN, 1L, 2L),
                "ANALYST should not be able to update ADMIN users");
        }
    }

    @Nested
    @DisplayName("canAccessAdminPages() tests")
    class CanAccessAdminPagesTests {
        
        @Test
        @DisplayName("SUPERADMIN can access admin pages")
        void superadminCanAccessAdminPages() {
            assertTrue(RoleChecker.canAccessAdminPages(UserRole.SUPERADMIN),
                "SUPERADMIN should be able to access admin pages");
        }
        
        @Test
        @DisplayName("ADMIN can access admin pages")
        void adminCanAccessAdminPages() {
            assertTrue(RoleChecker.canAccessAdminPages(UserRole.ADMIN),
                "ADMIN should be able to access admin pages");
        }
        
        @Test
        @DisplayName("ANALYST cannot access admin pages")
        void analystCannotAccessAdminPages() {
            assertFalse(RoleChecker.canAccessAdminPages(UserRole.ANALYST),
                "ANALYST should not be able to access admin pages");
        }
    }

    @Nested
    @DisplayName("canCreateUserWithRole() tests")
    class CanCreateUserWithRoleTests {
        
        @Test
        @DisplayName("No one can create SUPERADMIN")
        void noOneCanCreateSuperadmin() {
            assertFalse(RoleChecker.canCreateUserWithRole(UserRole.SUPERADMIN, UserRole.SUPERADMIN),
                "Cannot create SUPERADMIN programmatically");
            assertFalse(RoleChecker.canCreateUserWithRole(UserRole.ADMIN, UserRole.SUPERADMIN),
                "ADMIN cannot create SUPERADMIN");
            assertFalse(RoleChecker.canCreateUserWithRole(UserRole.ANALYST, UserRole.SUPERADMIN),
                "ANALYST cannot create SUPERADMIN");
        }
        
        @Test
        @DisplayName("SUPERADMIN can create ADMIN via this method")
        void superadminCanCreateAdminViaMethod() {
            assertTrue(RoleChecker.canCreateUserWithRole(UserRole.SUPERADMIN, UserRole.ADMIN),
                "SUPERADMIN should be able to create ADMIN users");
        }
        
        @Test
        @DisplayName("SUPERADMIN can create ANALYST via this method")
        void superadminCanCreateAnalystViaMethod() {
            assertTrue(RoleChecker.canCreateUserWithRole(UserRole.SUPERADMIN, UserRole.ANALYST),
                "SUPERADMIN should be able to create ANALYST users");
        }
        
        @Test
        @DisplayName("ADMIN can only create ANALYST via this method")
        void adminCanOnlyCreateAnalystViaMethod() {
            assertFalse(RoleChecker.canCreateUserWithRole(UserRole.ADMIN, UserRole.ADMIN),
                "ADMIN cannot create other ADMIN users via this method");
            assertTrue(RoleChecker.canCreateUserWithRole(UserRole.ADMIN, UserRole.ANALYST),
                "ADMIN should be able to create ANALYST users");
        }
        
        @Test
        @DisplayName("ANALYST cannot create anyone via this method")
        void analystCannotCreateAnyoneViaMethod() {
            assertFalse(RoleChecker.canCreateUserWithRole(UserRole.ANALYST, UserRole.ADMIN),
                "ANALYST cannot create ADMIN users");
            assertFalse(RoleChecker.canCreateUserWithRole(UserRole.ANALYST, UserRole.ANALYST),
                "ANALYST cannot create other ANALYST users");
        }
    }
}
