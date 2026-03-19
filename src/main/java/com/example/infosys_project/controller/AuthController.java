package com.example.infosys_project.controller;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            String token = authService.login(email, password);

            Optional<AdminUser> adminOpt = authService.getAdminFromToken(token);
            AdminUser admin = adminOpt.orElseThrow(() -> new RuntimeException("Invalid credentials"));

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("name", admin.getName());
            response.put("email", admin.getEmail());

            ResponseCookie cookie = ResponseCookie.from("fraud_session", token)
                    .path("/")
                    .httpOnly(true)
                    .sameSite("Lax")
                    .maxAge(Duration.ofHours(8))
                    .build();

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
                                       HttpServletResponse servletResponse) {
        String token = extractBearerToken(authorization);
        authService.logout(token);

        ResponseCookie cookie = ResponseCookie.from("fraud_session", "")
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

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
        response.put("lastLogin", admin.getLastLogin());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
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

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/update-credentials")
    public ResponseEntity<Map<String, Object>> updateCredentials(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
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
}
