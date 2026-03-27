package com.example.infosys_project.service;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.model.UserRole;
import com.example.infosys_project.repository.AdminRepository;
import jakarta.mail.internet.MimeMessage;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailAlertService}.
 * Tests email alert filtering logic - only opted-in SUPERADMIN/ADMIN users should receive alerts.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailAlertService Unit Tests")
class EmailAlertServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailAlertService emailAlertService;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private TransactionModel highRiskTransaction;
    private TransactionModel criticalRiskTransaction;
    private AdminUser superAdminOptedIn;
    private AdminUser adminOptedIn;
    private AdminUser adminOptedOut;
    private AdminUser analystOptedIn;
    private AdminUser inactiveAdminOptedIn;

    @BeforeEach
    void setUp() {
        // Set required fields for email service
        ReflectionTestUtils.setField(emailAlertService, "senderAddress", "alerts@fraudshield.com");
        ReflectionTestUtils.setField(emailAlertService, "senderName", "FraudShield Alerts");

        // Create test transaction - HIGH risk
        highRiskTransaction = new TransactionModel();
        highRiskTransaction.transactionId = "txn-high-001";
        highRiskTransaction.accountHolderName = "John Doe";
        highRiskTransaction.amount = 150000.0;
        highRiskTransaction.riskLevel = "HIGH";
        highRiskTransaction.riskScore = 7.5;
        highRiskTransaction.fraudReason = "R-01:HighAmount | R-05:VPNDetected";
        highRiskTransaction.location = "Mumbai, India";
        highRiskTransaction.merchantCategory = "Electronics";
        highRiskTransaction.transactionMode = "ONLINE";
        highRiskTransaction.ipRiskTag = "VPN";
        highRiskTransaction.mlFraudProbability = 0.85;
        highRiskTransaction.timestamp = LocalDateTime.now();
        highRiskTransaction.isFraud = true;

        // Create test transaction - CRITICAL risk
        criticalRiskTransaction = new TransactionModel();
        criticalRiskTransaction.transactionId = "txn-critical-001";
        criticalRiskTransaction.accountHolderName = "Jane Smith";
        criticalRiskTransaction.amount = 500000.0;
        criticalRiskTransaction.riskLevel = "CRITICAL";
        criticalRiskTransaction.riskScore = 9.8;
        criticalRiskTransaction.fraudReason = "R-01:HighAmount | R-07:TORDetected | R-ML:MLFlagged(prob=0.95)";
        criticalRiskTransaction.location = "Unknown";
        criticalRiskTransaction.merchantCategory = "Cryptocurrency";
        criticalRiskTransaction.transactionMode = "ONLINE";
        criticalRiskTransaction.ipRiskTag = "TOR";
        criticalRiskTransaction.mlFraudProbability = 0.95;
        criticalRiskTransaction.timestamp = LocalDateTime.now();
        criticalRiskTransaction.isFraud = true;

        // Create test users - SUPERADMIN opted in, active
        superAdminOptedIn = new AdminUser();
        superAdminOptedIn.setEmail("super@test.com");
        superAdminOptedIn.setRole(UserRole.SUPERADMIN);
        superAdminOptedIn.setEmailAlertsEnabled(true);
        superAdminOptedIn.setActive(true);

        // ADMIN opted in, active
        adminOptedIn = new AdminUser();
        adminOptedIn.setEmail("admin@test.com");
        adminOptedIn.setRole(UserRole.ADMIN);
        adminOptedIn.setEmailAlertsEnabled(true);
        adminOptedIn.setActive(true);

        // ADMIN opted out, active
        adminOptedOut = new AdminUser();
        adminOptedOut.setEmail("admin-noalerts@test.com");
        adminOptedOut.setRole(UserRole.ADMIN);
        adminOptedOut.setEmailAlertsEnabled(false);
        adminOptedOut.setActive(true);

        // ANALYST opted in, active (should NOT receive alerts)
        analystOptedIn = new AdminUser();
        analystOptedIn.setEmail("analyst@test.com");
        analystOptedIn.setRole(UserRole.ANALYST);
        analystOptedIn.setEmailAlertsEnabled(true);
        analystOptedIn.setActive(true);

        // ADMIN opted in, inactive
        inactiveAdminOptedIn = new AdminUser();
        inactiveAdminOptedIn.setEmail("inactive@test.com");
        inactiveAdminOptedIn.setRole(UserRole.ADMIN);
        inactiveAdminOptedIn.setEmailAlertsEnabled(true);
        inactiveAdminOptedIn.setActive(false);

        // Mock MimeMessage creation
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    @DisplayName("Email Recipient Filtering Tests")
    class EmailRecipientFilteringTests {

        @Test
        @DisplayName("Should send email only to opted-in SUPERADMIN and ADMIN users")
        void shouldSendEmailOnlyToOptedInSuperAdminAndAdmin() {
            // Given - repository returns all users, service should filter
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Arrays.asList(superAdminOptedIn, adminOptedIn, analystOptedIn));

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - only 2 emails sent (SUPERADMIN + ADMIN, not ANALYST)
            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should NOT send email to opted-out ADMIN users")
        void shouldNotSendEmailToOptedOutAdmins() {
            // Given - repository query findByIsActiveTrueAndEmailAlertsEnabledTrue
            // would not return opted-out users, so it returns empty list
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.emptyList());

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - no emails sent (no opted-in users found)
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should NOT send email to inactive users even if opted in")
        void shouldNotSendEmailToInactiveUsers() {
            // Given - inactive user should be filtered by repository query
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.emptyList());

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - no emails sent
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should NOT send email to ANALYST users even if opted in")
        void shouldNotSendEmailToAnalysts() {
            // Given - only analyst available
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(analystOptedIn));

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - no emails sent (analysts don't get alerts)
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should send to multiple eligible recipients")
        void shouldSendToMultipleEligibleRecipients() {
            // Given - 2 admins + 1 superadmin, all opted in and active
            AdminUser admin2 = new AdminUser();
            admin2.setEmail("admin2@test.com");
            admin2.setRole(UserRole.ADMIN);
            admin2.setEmailAlertsEnabled(true);
            admin2.setActive(true);

            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Arrays.asList(superAdminOptedIn, adminOptedIn, admin2));

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - 3 emails sent
            verify(mailSender, times(3)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle no eligible recipients gracefully")
        void shouldHandleNoEligibleRecipients() {
            // Given - no eligible recipients
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.emptyList());

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - no emails sent, no exception
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Email Content Tests")
    class EmailContentTests {

        @Test
        @DisplayName("Should use async execution for email sending")
        void shouldBeAsyncMethod() throws NoSuchMethodException {
            // Verify the method is annotated with @Async
            var method = EmailAlertService.class.getDeclaredMethod("sendFraudAlert", TransactionModel.class);
            assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class)).isTrue();
        }

        @Test
        @DisplayName("Should handle CRITICAL risk level transactions")
        void shouldHandleCriticalRiskTransactions() {
            // Given
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(superAdminOptedIn));

            // When
            emailAlertService.sendFraudAlert(criticalRiskTransaction);

            // Then - email sent for critical transaction
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle HIGH risk level transactions")
        void shouldHandleHighRiskTransactions() {
            // Given
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(adminOptedIn));

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - email sent for high risk transaction
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle transaction with null risk level")
        void shouldHandleNullRiskLevel() {
            // Given
            TransactionModel txWithNullRisk = new TransactionModel();
            txWithNullRisk.transactionId = "txn-null-risk";
            txWithNullRisk.accountHolderName = "Test User";
            txWithNullRisk.amount = 10000.0;
            txWithNullRisk.riskLevel = null; // null risk level
            txWithNullRisk.riskScore = 5.0;
            txWithNullRisk.fraudReason = "None";
            txWithNullRisk.location = "Test Location";
            txWithNullRisk.merchantCategory = "Test";
            txWithNullRisk.transactionMode = "ONLINE";
            txWithNullRisk.ipRiskTag = "CLEAN";
            txWithNullRisk.mlFraudProbability = 0.5;
            txWithNullRisk.timestamp = LocalDateTime.now();

            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(adminOptedIn));

            // When
            emailAlertService.sendFraudAlert(txWithNullRisk);

            // Then - should default to HIGH and send email
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle transaction with null timestamp")
        void shouldHandleNullTimestamp() {
            // Given
            TransactionModel txWithNullTimestamp = new TransactionModel();
            txWithNullTimestamp.transactionId = "txn-no-time";
            txWithNullTimestamp.accountHolderName = "Test User";
            txWithNullTimestamp.amount = 50000.0;
            txWithNullTimestamp.riskLevel = "HIGH";
            txWithNullTimestamp.riskScore = 6.5;
            txWithNullTimestamp.fraudReason = "R-01:HighAmount";
            txWithNullTimestamp.location = "Test";
            txWithNullTimestamp.merchantCategory = "Test";
            txWithNullTimestamp.transactionMode = "ONLINE";
            txWithNullTimestamp.ipRiskTag = "CLEAN";
            txWithNullTimestamp.mlFraudProbability = 0.6;
            txWithNullTimestamp.timestamp = null; // null timestamp

            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(superAdminOptedIn));

            // When
            emailAlertService.sendFraudAlert(txWithNullTimestamp);

            // Then - should handle gracefully and send email
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should continue sending to other recipients if one fails")
        void shouldContinueOnIndividualFailure() {
            // Given
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Arrays.asList(superAdminOptedIn, adminOptedIn));

            // Simulate failure for first email only
            doThrow(new RuntimeException("SMTP error"))
                    .doNothing()
                    .when(mailSender).send(any(MimeMessage.class));

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - both attempts made despite first failure
            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle repository exception gracefully")
        void shouldHandleRepositoryException() {
            // Given
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenThrow(new RuntimeException("Database connection error"));

            // When - should not throw exception
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - no emails sent, but no exception propagated
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Repository Query Validation Tests")
    class RepositoryQueryValidationTests {

        @Test
        @DisplayName("Should query only active users with email alerts enabled")
        void shouldQueryOnlyActiveUsersWithAlertsEnabled() {
            // Given
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(adminOptedIn));

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - correct repository method called
            verify(adminRepository, times(1)).findByIsActiveTrueAndEmailAlertsEnabledTrue();
        }
    }

    @Nested
    @DisplayName("Mixed Scenario Tests")
    class MixedScenarioTests {

        @Test
        @DisplayName("Should filter correctly with mixed user roles and settings")
        void shouldFilterMixedUsers() {
            // Given - mix of all user types
            List<AdminUser> mixedUsers = Arrays.asList(
                    superAdminOptedIn,    // ✓ should receive
                    adminOptedIn,         // ✓ should receive
                    adminOptedOut,        // ✗ opted out
                    analystOptedIn,       // ✗ analyst role
                    inactiveAdminOptedIn  // ✗ inactive
            );

            // Repository should only return active + opted-in users
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Arrays.asList(superAdminOptedIn, adminOptedIn, analystOptedIn));

            // When
            emailAlertService.sendFraudAlert(criticalRiskTransaction);

            // Then - only 2 emails sent (SUPERADMIN + ADMIN, filtered out ANALYST)
            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle edge case with only SUPERADMIN opted in")
        void shouldHandleOnlySuperAdmin() {
            // Given - only superadmin opted in
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(superAdminOptedIn));

            // When
            emailAlertService.sendFraudAlert(highRiskTransaction);

            // Then - 1 email sent
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle edge case with only ADMIN opted in")
        void shouldHandleOnlyAdmin() {
            // Given - only admin opted in
            when(adminRepository.findByIsActiveTrueAndEmailAlertsEnabledTrue())
                    .thenReturn(Collections.singletonList(adminOptedIn));

            // When
            emailAlertService.sendFraudAlert(criticalRiskTransaction);

            // Then - 1 email sent
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }
    }
}
