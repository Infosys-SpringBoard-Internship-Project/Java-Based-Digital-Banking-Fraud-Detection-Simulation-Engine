package com.example.infosys_project.service;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.UserRole;
import com.example.infosys_project.repository.AdminRepository;
import com.example.infosys_project.security.RoleChecker;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    public enum PasswordResetStatus {
        RESET_SENT,
        NOT_FOUND,
        INACTIVE,
        ROLE_MISMATCH,
        EMAIL_FAILED
    }

    private static final int SESSION_HOURS = 8;
    private static final int EMAIL_SEND_ATTEMPTS = 3;
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$%!";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailAlertService emailAlertService;

    @Value("${spring.mail.username:no-reply@fraudshield.local}")
    private String mailFrom;

    private final Map<String, AdminUser> sessions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionExpiry = new ConcurrentHashMap<>();

    public String login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        AdminUser admin = adminRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!admin.isActive()) {
            throw new RuntimeException("Admin account is inactive");
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        String token = UUID.randomUUID().toString();
        sessions.put(token, admin);
        sessionExpiry.put(token, LocalDateTime.now().plusHours(SESSION_HOURS));
        return token;
    }

    public boolean isPasswordChangeRequired(String token) {
        AdminUser admin = getAdminFromToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        return Boolean.TRUE.equals(admin.getMustChangePassword());
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessions.remove(token);
        sessionExpiry.remove(token);
    }

    public Optional<AdminUser> getAdminFromToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        AdminUser admin = sessions.get(token);
        LocalDateTime expiry = sessionExpiry.get(token);

        if (admin == null || expiry == null) {
            return Optional.empty();
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            logout(token);
            return Optional.empty();
        }

        return Optional.of(admin);
    }

    public AdminUser createAdmin(String name, String email, String rawPassword) {
        AdminUser admin = new AdminUser();
        admin.setName(name);
        admin.setEmail(email == null ? null : email.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(rawPassword));
        boolean firstAccount = !hasAnyAdmin();
        admin.setRole(firstAccount ? UserRole.SUPERADMIN : UserRole.ADMIN);
        admin.setActive(true);
        admin.setEmailAlertsEnabled(true);
        admin.setCreatedBy(firstAccount ? "SYSTEM" : null);
        admin.setCanBeDeleted(!firstAccount);
        return adminRepository.save(admin);
    }

    public boolean hasAnyAdmin() {
        return adminRepository.count() > 0;
    }

    public AdminUser updateCredentials(String token,
                                       String currentPassword,
                                       String newEmail,
                                       String newPassword) {
        AdminUser admin = getAdminFromToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        boolean mustChangePassword = Boolean.TRUE.equals(admin.getMustChangePassword());
        if (!mustChangePassword) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new RuntimeException("Current password is required");
            }

            if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
        }

        String normalizedEmail = newEmail == null ? "" : newEmail.trim().toLowerCase();
        String normalizedPassword = newPassword == null ? "" : newPassword.trim();

        if (normalizedEmail.isEmpty() && normalizedPassword.isEmpty()) {
            throw new RuntimeException("Provide new email or new password");
        }

        if (!normalizedEmail.isEmpty() && !normalizedEmail.equalsIgnoreCase(admin.getEmail())) {
            adminRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                if (!existing.getId().equals(admin.getId())) {
                    throw new RuntimeException("Email already in use");
                }
            });
            admin.setEmail(normalizedEmail);
        }

        if (!normalizedPassword.isEmpty()) {
            if (normalizedPassword.length() < 6) {
                throw new RuntimeException("New password must be at least 6 characters");
            }
            admin.setPassword(passwordEncoder.encode(normalizedPassword));
            admin.setMustChangePassword(false);
        }

        AdminUser saved = adminRepository.save(admin);
        sessions.put(token, saved);
        return saved;
    }

    public PasswordResetStatus resetPasswordAndNotify(String email, UserRole roleHint) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isEmpty()) {
            return PasswordResetStatus.NOT_FOUND;
        }

        Optional<AdminUser> adminOpt = adminRepository.findByEmail(normalizedEmail);
        if (adminOpt.isEmpty()) {
            return PasswordResetStatus.NOT_FOUND;
        }

        AdminUser admin = adminOpt.get();
        if (!admin.isActive()) {
            return PasswordResetStatus.INACTIVE;
        }

        if (roleHint != null && !matchesRoleHint(admin.getRole(), roleHint)) {
            return PasswordResetStatus.ROLE_MISMATCH;
        }

        String temporaryPassword = generateTemporaryPassword();
        boolean emailSent = sendTemporaryPasswordEmail(admin, temporaryPassword);
        if (!emailSent) {
            return PasswordResetStatus.EMAIL_FAILED;
        }

        admin.setPassword(passwordEncoder.encode(temporaryPassword));
        admin.setMustChangePassword(true);
        adminRepository.save(admin);

        // Invalidate active sessions so the new password takes effect immediately.
        sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(admin.getId()));
        sessionExpiry.entrySet().removeIf(entry -> !sessions.containsKey(entry.getKey()));

        return PasswordResetStatus.RESET_SENT;
    }

    private boolean matchesRoleHint(UserRole actualRole, UserRole roleHint) {
        if (actualRole == null || roleHint == null) {
            return false;
        }
        if (roleHint == UserRole.ADMIN) {
            return actualRole == UserRole.ADMIN || actualRole == UserRole.SUPERADMIN;
        }
        return actualRole == roleHint;
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int idx = secureRandom.nextInt(TEMP_PASSWORD_CHARS.length());
            builder.append(TEMP_PASSWORD_CHARS.charAt(idx));
        }
        return builder.toString();
    }

    private boolean sendTemporaryPasswordEmail(AdminUser admin, String temporaryPassword) {
        String roleLabel = admin.getRole() == null ? "USER" : admin.getRole().name();
        String htmlBody = emailAlertService.buildForgotPasswordEmailHtml(admin, temporaryPassword);

        for (int attempt = 1; attempt <= EMAIL_SEND_ATTEMPTS; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(mailFrom);
                helper.setTo(admin.getEmail());
                helper.setSubject("FraudShield " + roleLabel + " Temporary Password");
                helper.setText(htmlBody, true);
                mailSender.send(message);

                if (attempt > 1) {
                    log.info("Password reset email sent to {} on retry attempt {}", admin.getEmail(), attempt);
                }
                return true;
            } catch (Exception ex) {
                if (attempt == EMAIL_SEND_ATTEMPTS) {
                    log.error("Failed to send password reset email to {} after {} attempts. mailFrom='{}', reason='{}'",
                            admin.getEmail(), EMAIL_SEND_ATTEMPTS, mailFrom, ex.getMessage(), ex);
                    return false;
                }

                log.warn("Password reset email attempt {}/{} failed for {}: {}",
                        attempt, EMAIL_SEND_ATTEMPTS, admin.getEmail(), ex.getMessage());
                try {
                    Thread.sleep(750L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return false;
    }

    // ==================== USER MANAGEMENT METHODS ====================

    /**
     * Create a new user (ADMIN or ANALYST)
     * Only SUPERADMIN can create ADMIN, SUPERADMIN and ADMIN can create ANALYST
     */
    public AdminUser createUser(String token, String name, String email, String rawPassword, UserRole role) {
        AdminUser creator = getAdminFromToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        // Validate permission to create user with specified role
        if (!RoleChecker.canCreateUserWithRole(creator.getRole(), role)) {
            throw new RuntimeException("You don't have permission to create " + role + " users");
        }

        // Check if email already exists
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Validate password
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        AdminUser newUser = new AdminUser();
        newUser.setName(name);
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRole(role);
        newUser.setActive(true);
        newUser.setEmailAlertsEnabled(role == UserRole.ADMIN || role == UserRole.SUPERADMIN);
        newUser.setCreatedBy(creator.getEmail());
        newUser.setCanBeDeleted(role != UserRole.SUPERADMIN);

        return adminRepository.save(newUser);
    }

    /**
     * Get all users (ADMIN+ only)
     */
    public java.util.List<AdminUser> getAllUsers(String token) {
        AdminUser requester = getAdminFromToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        if (!RoleChecker.canManageUsers(requester.getRole())) {
            throw new RuntimeException("You don't have permission to view users");
        }

        java.util.List<AdminUser> users = adminRepository.findAll();
        if (requester.getRole() == UserRole.ADMIN) {
            return users.stream()
                    .filter(user -> user.getRole() != UserRole.SUPERADMIN)
                    .toList();
        }
        return users;
    }

    /**
     * Delete a user
     * SUPERADMIN can delete ADMIN/ANALYST, ADMIN can delete ANALYST only
     */
    public void deleteUser(String token, Long targetUserId) {
        AdminUser deleter = getAdminFromToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        AdminUser targetUser = adminRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!RoleChecker.canDeleteUser(deleter.getRole(), targetUser.getRole(), targetUser.getCanBeDeleted())) {
            throw new RuntimeException("You don't have permission to delete this user");
        }

        // Prevent deleting yourself
        if (deleter.getId().equals(targetUserId)) {
            throw new RuntimeException("You cannot delete yourself");
        }

        // Invalidate all sessions for the deleted user
        sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(targetUserId));
        sessionExpiry.entrySet().removeIf(entry -> !sessions.containsKey(entry.getKey()));

        adminRepository.delete(targetUser);
    }

    /**
     * Update user (role, email alerts, active status)
     */
    public AdminUser updateUser(String token, Long targetUserId, UserRole newRole, Boolean emailAlerts, Boolean isActive) {
        AdminUser updater = getAdminFromToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        AdminUser targetUser = adminRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!RoleChecker.canUpdateUser(updater.getRole(), targetUser.getRole(), updater.getId(), targetUserId)) {
            throw new RuntimeException("You don't have permission to update this user");
        }

        // Update role (only if different and permission allows)
        if (newRole != null && newRole != targetUser.getRole()) {
            if (targetUser.getRole() == UserRole.SUPERADMIN) {
                throw new RuntimeException("Cannot change SUPERADMIN role");
            }
            if (!RoleChecker.canCreateUserWithRole(updater.getRole(), newRole)) {
                throw new RuntimeException("You don't have permission to assign " + newRole + " role");
            }
            targetUser.setRole(newRole);
        }

        // Update email alerts
        if (emailAlerts != null) {
            targetUser.setEmailAlertsEnabled(emailAlerts);
        }

        // Update active status (cannot deactivate superadmin)
        if (isActive != null && targetUser.getRole() != UserRole.SUPERADMIN) {
            targetUser.setActive(isActive);
            
            // If deactivating, invalidate sessions
            if (!isActive) {
                sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(targetUserId));
                sessionExpiry.entrySet().removeIf(entry -> !sessions.containsKey(entry.getKey()));
            }
        }

        return adminRepository.save(targetUser);
    }

    /**
     * Toggle email alerts for current user
     */
    public AdminUser toggleEmailAlerts(String token) {
        AdminUser admin = getAdminFromToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        admin.setEmailAlertsEnabled(!admin.getEmailAlertsEnabled());
        AdminUser saved = adminRepository.save(admin);
        
        // Update session
        sessions.put(token, saved);
        
        return saved;
    }

    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        // Clean expired sessions first
        LocalDateTime now = LocalDateTime.now();
        sessionExpiry.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        sessions.entrySet().removeIf(entry -> !sessionExpiry.containsKey(entry.getKey()));
        
        return sessions.size();
    }
}
