package com.example.infosys_project.service;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.AuditLog;
import com.example.infosys_project.model.UserRole;
import com.example.infosys_project.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditService}.
 * Tests audit log creation for various user actions and scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AdminUser testSuperAdmin;
    private AdminUser testAdmin;
    private AdminUser testAnalyst;

    @BeforeEach
    void setUp() {
        testSuperAdmin = new AdminUser();
        testSuperAdmin.setEmail("super@test.com");
        testSuperAdmin.setRole(UserRole.SUPERADMIN);

        testAdmin = new AdminUser();
        testAdmin.setEmail("admin@test.com");
        testAdmin.setRole(UserRole.ADMIN);

        testAnalyst = new AdminUser();
        testAnalyst.setEmail("analyst@test.com");
        testAnalyst.setRole(UserRole.ANALYST);
    }

    @Nested
    @DisplayName("Audit Log Creation Tests")
    class AuditLogCreationTests {

        @Test
        @DisplayName("Should create audit log with all fields for valid user")
        void shouldCreateAuditLogWithAllFields() {
            // Given
            String actionType = "CREATE_USER";
            String targetEntity = "USER";
            String targetId = "user-123";
            String details = "Created new ANALYST user";
            String ipAddress = "192.168.1.100";
            String userAgent = "Mozilla/5.0";

            // When
            auditService.log(testAdmin, actionType, targetEntity, targetId, details, ipAddress, userAgent);

            // Then
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getTimestamp()).isNotNull();
            assertThat(savedLog.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
            assertThat(savedLog.getUserEmail()).isEqualTo("admin@test.com");
            assertThat(savedLog.getUserRole()).isEqualTo("ADMIN");
            assertThat(savedLog.getActionType()).isEqualTo("CREATE_USER");
            assertThat(savedLog.getTargetEntity()).isEqualTo("USER");
            assertThat(savedLog.getTargetId()).isEqualTo("user-123");
            assertThat(savedLog.getDetails()).isEqualTo("Created new ANALYST user");
            assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
            assertThat(savedLog.getUserAgent()).isEqualTo("Mozilla/5.0");
        }

        @Test
        @DisplayName("Should handle null actor gracefully")
        void shouldHandleNullActor() {
            // When
            auditService.log(null, "LOGIN_ATTEMPT", "AUTH", null, "Failed login", "10.0.0.1", "Chrome");

            // Then
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getUserEmail()).isEqualTo("ANONYMOUS");
            assertThat(savedLog.getUserRole()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("Should handle actor with null role")
        void shouldHandleActorWithNullRole() {
            // Given
            AdminUser userWithoutRole = new AdminUser();
            userWithoutRole.setEmail("noRole@test.com");
            userWithoutRole.setRole(null);

            // When
            auditService.log(userWithoutRole, "VIEW_DATA", "TRANSACTION", "txn-456", "Viewed transaction", "127.0.0.1", "Firefox");

            // Then
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getUserEmail()).isEqualTo("noRole@test.com");
            assertThat(savedLog.getUserRole()).isEqualTo("UNKNOWN");
        }
    }

    @Nested
    @DisplayName("Action Type Coverage Tests")
    class ActionTypeCoverageTests {

        @Test
        @DisplayName("Should log LOGIN action")
        void shouldLogLoginAction() {
            auditService.log(testAnalyst, "LOGIN", "AUTH", null, "User logged in", "192.168.1.50", "Safari");
            verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Should log LOGOUT action")
        void shouldLogLogoutAction() {
            auditService.log(testAnalyst, "LOGOUT", "AUTH", null, "User logged out", "192.168.1.50", "Safari");
            verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Should log CREATE_USER action")
        void shouldLogCreateUserAction() {
            auditService.log(testAdmin, "CREATE_USER", "USER", "new-user-789", "Created ANALYST user", "10.0.0.5", "Edge");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getActionType()).isEqualTo("CREATE_USER");
            assertThat(savedLog.getTargetEntity()).isEqualTo("USER");
            assertThat(savedLog.getTargetId()).isEqualTo("new-user-789");
        }

        @Test
        @DisplayName("Should log DELETE_USER action")
        void shouldLogDeleteUserAction() {
            auditService.log(testSuperAdmin, "DELETE_USER", "USER", "deleted-user-999", "Deleted user", "172.16.0.1", "Chrome");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getActionType()).isEqualTo("DELETE_USER");
            assertThat(savedLog.getUserRole()).isEqualTo("SUPERADMIN");
        }

        @Test
        @DisplayName("Should log MANUAL_VALIDATE action")
        void shouldLogManualValidateAction() {
            auditService.log(testAdmin, "MANUAL_VALIDATE", "TRANSACTION", "txn-001", "Manual transaction validation", "192.168.1.1", "PostmanRuntime");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getActionType()).isEqualTo("MANUAL_VALIDATE");
            assertThat(savedLog.getTargetEntity()).isEqualTo("TRANSACTION");
        }

        @Test
        @DisplayName("Should log EXPORT_CSV action")
        void shouldLogExportCsvAction() {
            auditService.log(testAnalyst, "EXPORT_FILTERED_CSV", "TRANSACTION", null, "Exported filtered transaction CSV", "10.10.10.10", "curl");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getActionType()).isEqualTo("EXPORT_FILTERED_CSV");
            assertThat(savedLog.getUserRole()).isEqualTo("ANALYST");
        }

        @Test
        @DisplayName("Should log VIEW_TRANSACTION action")
        void shouldLogViewTransactionAction() {
            auditService.log(testAnalyst, "VIEW_TRANSACTION", "TRANSACTION", "txn-12345", "Viewed transaction details", "192.168.100.50", "Chrome");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getDetails()).contains("Viewed transaction");
        }

        @Test
        @DisplayName("Should log SEARCH action")
        void shouldLogSearchAction() {
            auditService.log(testAdmin, "SEARCH", "TRANSACTION", null, "Searched transactions with filters", "172.20.0.10", "Firefox");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getActionType()).isEqualTo("SEARCH");
        }
    }

    @Nested
    @DisplayName("Role-Based Audit Tests")
    class RoleBasedAuditTests {

        @Test
        @DisplayName("SUPERADMIN actions should be logged correctly")
        void superadminActionsShouldBeLogged() {
            auditService.log(testSuperAdmin, "SYSTEM_CONFIG", "SETTINGS", "config-1", "Updated system settings", "127.0.0.1", "Admin Panel");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getUserRole()).isEqualTo("SUPERADMIN");
            assertThat(savedLog.getUserEmail()).isEqualTo("super@test.com");
        }

        @Test
        @DisplayName("ADMIN actions should be logged correctly")
        void adminActionsShouldBeLogged() {
            auditService.log(testAdmin, "CREATE_ANALYST", "USER", "analyst-new", "Created new analyst", "10.0.0.1", "Chrome");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getUserRole()).isEqualTo("ADMIN");
            assertThat(savedLog.getUserEmail()).isEqualTo("admin@test.com");
        }

        @Test
        @DisplayName("ANALYST actions should be logged correctly")
        void analystActionsShouldBeLogged() {
            auditService.log(testAnalyst, "VIEW_DASHBOARD", "ANALYTICS", null, "Viewed analytics dashboard", "192.168.50.50", "Safari");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getUserRole()).isEqualTo("ANALYST");
            assertThat(savedLog.getUserEmail()).isEqualTo("analyst@test.com");
        }
    }

    @Nested
    @DisplayName("Timestamp Validation Tests")
    class TimestampValidationTests {

        @Test
        @DisplayName("Timestamp should be set to current time")
        void timestampShouldBeCurrentTime() {
            // Given
            LocalDateTime beforeLog = LocalDateTime.now();

            // When
            auditService.log(testAdmin, "TEST_ACTION", "TEST", "123", "Test", "127.0.0.1", "Test");

            // Then
            LocalDateTime afterLog = LocalDateTime.now();
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getTimestamp()).isNotNull();
            assertThat(savedLog.getTimestamp()).isBetween(beforeLog, afterLog);
        }
    }

    @Nested
    @DisplayName("Special Character Handling Tests")
    class SpecialCharacterHandlingTests {

        @Test
        @DisplayName("Should handle special characters in details")
        void shouldHandleSpecialCharactersInDetails() {
            String detailsWithSpecialChars = "User created with email: test+user@example.com & role: ANALYST (via API)";
            
            auditService.log(testAdmin, "CREATE_USER", "USER", "user-special", detailsWithSpecialChars, "192.168.1.1", "API Client");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            assertThat(savedLog.getDetails()).isEqualTo(detailsWithSpecialChars);
        }

        @Test
        @DisplayName("Should handle SQL injection attempt in details")
        void shouldHandleSqlInjectionInDetails() {
            String maliciousDetails = "'; DROP TABLE audit_logs; --";
            
            auditService.log(testAdmin, "MALICIOUS_ATTEMPT", "SECURITY", null, maliciousDetails, "1.2.3.4", "Attacker");
            
            verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
            AuditLog savedLog = auditLogCaptor.getValue();
            // Should be stored as-is, let JPA/Hibernate handle escaping
            assertThat(savedLog.getDetails()).isEqualTo(maliciousDetails);
        }
    }
}
