package com.example.infosys_project.controller;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.UserRole;
import com.example.infosys_project.service.AuditService;
import com.example.infosys_project.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            String roleRaw = body.get("role");
            String token = authService.login(email, password);

            Optional<AdminUser> adminOpt = authService.getAdminFromToken(token);
            AdminUser admin = adminOpt.orElseThrow(() -> new RuntimeException("Invalid credentials"));

            if (roleRaw != null && !roleRaw.isBlank()) {
                UserRole selectedRole;
                try {
                    selectedRole = UserRole.valueOf(roleRaw.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException("Invalid credentials");
                }
                boolean roleAllowed = selectedRole == UserRole.ADMIN
                        ? (admin.getRole() == UserRole.ADMIN || admin.getRole() == UserRole.SUPERADMIN)
                        : admin.getRole() == selectedRole;
                if (!roleAllowed) {
                    authService.logout(token);
                    throw new RuntimeException("Invalid credentials");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("name", admin.getName());
            response.put("email", admin.getEmail());
            response.put("role", admin.getRole());
            response.put("mustChangePassword", Boolean.TRUE.equals(admin.getMustChangePassword()));

            ResponseCookie cookie = ResponseCookie.from("fraud_session", token)
                    .path("/")
                    .httpOnly(true)
                    .sameSite("Lax")
                    .maxAge(Duration.ofHours(8))
                    .build();

            auditService.log(admin, "LOGIN", "AUTH", token,
                    "Successful login", request.getRemoteAddr(), request.getHeader("User-Agent"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       HttpServletResponse servletResponse,
                                       HttpServletRequest request) {
        String token = extractBearerToken(authorization);
        AdminUser actor = authService.getAdminFromToken(token).orElse(null);
        authService.logout(token);

        ResponseCookie cookie = ResponseCookie.from("fraud_session", "")
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        if (actor != null) {
            auditService.log(actor, "LOGOUT", "AUTH", token,
                    "User logged out", request.getRemoteAddr(), request.getHeader("User-Agent"));
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        Optional<AdminUser> adminOpt = authService.getAdminFromToken(token);
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AdminUser admin = adminOpt.get();
        Map<String, Object> response = new HashMap<>();
            response.put("name", admin.getName());
            response.put("email", admin.getEmail());
            response.put("role", admin.getRole());
            response.put("emailAlertsEnabled", admin.getEmailAlertsEnabled());
            response.put("createdBy", admin.getCreatedBy());
            response.put("createdAt", admin.getCreatedAt());
            response.put("isActive", admin.isActive());
            response.put("canBeDeleted", admin.getCanBeDeleted());
            response.put("lastLogin", admin.getLastLogin());
            response.put("mustChangePassword", admin.getMustChangePassword());
            return ResponseEntity.ok(response);
    }

    @GetMapping("/bootstrap-status")
    public ResponseEntity<Map<String, Object>> bootstrapStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("setupRequired", !authService.hasAnyAdmin());
        response.put("superadminCreationAllowed", !authService.hasAnyAdmin());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/must-change-password")
    public ResponseEntity<Map<String, Object>> mustChangePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean required = authService.isPasswordChangeRequired(token);
            Map<String, Object> response = new HashMap<>();
            response.put("mustChangePassword", required);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body,
                                                        HttpServletRequest request) {
        if (authService.hasAnyAdmin()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Admin already exists");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        AdminUser admin = authService.createAdmin(
                body.get("name"),
                body.get("email"),
                body.get("password")
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", admin.getId());
        response.put("name", admin.getName());
        response.put("email", admin.getEmail());
        response.put("role", admin.getRole());
        response.put("createdAt", admin.getCreatedAt());

        auditService.log(admin, "INITIAL_SETUP", "USER", String.valueOf(admin.getId()),
                "Initial superadmin account created", request.getRemoteAddr(), request.getHeader("User-Agent"));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String roleHintRaw = body.get("role");
        if (email == null || email.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Email is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            UserRole roleHint = null;
            if (roleHintRaw != null && !roleHintRaw.isBlank()) {
                try {
                    roleHint = UserRole.valueOf(roleHintRaw.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    roleHint = null;
                }
            }
            AuthService.PasswordResetStatus status = authService.resetPasswordAndNotify(email, roleHint);

            Map<String, Object> response = new HashMap<>();
            response.put("status", status.name());
            response.put("role", roleHint == null ? "UNKNOWN" : roleHint.name());
            switch (status) {
                case RESET_SENT -> response.put("message", "Temporary password sent for selected role.");
                case INACTIVE -> response.put("message", "Selected role account is inactive.");
                case EMAIL_FAILED -> response.put("message", "Reset generated, but email delivery failed. Contact administrator.");
                case ROLE_MISMATCH, NOT_FOUND -> response.put("message", "No account found for selected role.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Unable to process reset request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/update-credentials")
    public ResponseEntity<Map<String, Object>> updateCredentials(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String token = extractBearerToken(authorization);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AdminUser updated = authService.updateCredentials(
                    token,
                    body.get("currentPassword"),
                    body.get("newEmail"),
                    body.get("newPassword")
            );

            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("name", updated.getName());
            response.put("email", updated.getEmail());
            response.put("role", updated.getRole());
            response.put("updated", true);

            auditService.log(updated, "UPDATE_CREDENTIALS", "USER", String.valueOf(updated.getId()),
                    "Updated login email/password", request.getRemoteAddr(), request.getHeader("User-Agent"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? "Update failed" : ex.getMessage();
            HttpStatus status = "Unauthorized".equalsIgnoreCase(message)
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.BAD_REQUEST;
            Map<String, Object> error = new HashMap<>();
            error.put("error", message);
            return ResponseEntity.status(status).body(error);
        }
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    // ==================== USER MANAGEMENT ENDPOINTS ====================

    /**
     * GET /auth/users - List all users (ADMIN+ only)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        try {
            java.util.List<AdminUser> users = authService.getAllUsers(token);
            java.util.List<Map<String, Object>> response = new java.util.ArrayList<>();
            
            for (AdminUser user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("name", user.getName());
                userMap.put("email", user.getEmail());
                userMap.put("role", user.getRole());
                userMap.put("emailAlertsEnabled", user.getEmailAlertsEnabled());
                userMap.put("createdBy", user.getCreatedBy());
                userMap.put("canBeDeleted", user.getCanBeDeleted());
                userMap.put("isActive", user.isActive());
                userMap.put("createdAt", user.getCreatedAt());
                userMap.put("lastLogin", user.getLastLogin());
                response.add(userMap);
            }
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    /**
     * POST /auth/create-user - Create new user (ADMIN+ can create ANALYST, SUPERADMIN can create both)
     */
    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String token = extractBearerToken(authorization);
        try {
            String name = body.get("name");
            String email = body.get("email");
            String password = body.get("password");
            String roleStr = body.get("role");
            
            if (name == null || email == null || password == null || roleStr == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Name, email, password, and role are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            UserRole role;
            try {
                role = UserRole.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid role. Must be ADMIN or ANALYST");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            AdminUser newUser = authService.createUser(token, name, email, password, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", newUser.getId());
            response.put("name", newUser.getName());
            response.put("email", newUser.getEmail());
            response.put("role", newUser.getRole());
            response.put("createdBy", newUser.getCreatedBy());
            response.put("createdAt", newUser.getCreatedAt());

            AdminUser actor = authService.getAdminFromToken(token).orElse(null);
            auditService.log(actor, "CREATE_USER", "USER", String.valueOf(newUser.getId()),
                    "Created " + newUser.getRole() + " account for " + newUser.getEmail(),
                    request.getRemoteAddr(), request.getHeader("User-Agent"));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", ex.getMessage());
            HttpStatus status = ex.getMessage().contains("Unauthorized") 
                ? HttpStatus.UNAUTHORIZED 
                : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * PUT /auth/users/{id} - Update user (role, email alerts, active status)
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String token = extractBearerToken(authorization);
        try {
            UserRole newRole = null;
            if (body.containsKey("role") && body.get("role") != null) {
                try {
                    newRole = UserRole.valueOf(body.get("role").toString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Invalid role");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }
            
            Boolean emailAlerts = body.containsKey("emailAlertsEnabled") 
                ? (Boolean) body.get("emailAlertsEnabled") 
                : null;
            Boolean isActive = body.containsKey("isActive") 
                ? (Boolean) body.get("isActive") 
                : null;
            
            AdminUser updated = authService.updateUser(token, id, newRole, emailAlerts, isActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("name", updated.getName());
            response.put("email", updated.getEmail());
            response.put("role", updated.getRole());
            response.put("emailAlertsEnabled", updated.getEmailAlertsEnabled());
            response.put("isActive", updated.isActive());

            AdminUser actor = authService.getAdminFromToken(token).orElse(null);
            auditService.log(actor, "UPDATE_USER", "USER", String.valueOf(updated.getId()),
                    "Updated role/alerts/status for " + updated.getEmail(),
                    request.getRemoteAddr(), request.getHeader("User-Agent"));
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", ex.getMessage());
            HttpStatus status = ex.getMessage().contains("Unauthorized") 
                ? HttpStatus.UNAUTHORIZED 
                : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * DELETE /auth/users/{id} - Delete user
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            HttpServletRequest request) {
        String token = extractBearerToken(authorization);
        try {
            AdminUser actor = authService.getAdminFromToken(token).orElse(null);
            authService.deleteUser(token, id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");

            auditService.log(actor, "DELETE_USER", "USER", String.valueOf(id),
                    "Deleted user account", request.getRemoteAddr(), request.getHeader("User-Agent"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", ex.getMessage());
            HttpStatus status = ex.getMessage().contains("Unauthorized") 
                ? HttpStatus.UNAUTHORIZED 
                : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * PUT /auth/toggle-alerts - Toggle email alerts for current user
     */
    @PutMapping("/toggle-alerts")
    public ResponseEntity<?> toggleEmailAlerts(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               HttpServletRequest request) {
        String token = extractBearerToken(authorization);
        try {
            AdminUser updated = authService.toggleEmailAlerts(token);
            Map<String, Object> response = new HashMap<>();
            response.put("emailAlertsEnabled", updated.getEmailAlertsEnabled());
            response.put("message", updated.getEmailAlertsEnabled() 
                ? "Email alerts enabled" 
                : "Email alerts disabled");

            auditService.log(updated, "TOGGLE_ALERTS", "USER", String.valueOf(updated.getId()),
                    "Email alerts set to " + updated.getEmailAlertsEnabled(),
                    request.getRemoteAddr(), request.getHeader("User-Agent"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}
