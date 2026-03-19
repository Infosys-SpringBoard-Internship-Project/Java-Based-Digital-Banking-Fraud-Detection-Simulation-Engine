package com.example.infosys_project.service;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final int SESSION_HOURS = 8;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private final Map<String, AdminUser> sessions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionExpiry = new ConcurrentHashMap<>();

    public String login(String email, String password) {
        AdminUser admin = adminRepository.findByEmail(email)
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
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole("ADMIN");
        admin.setActive(true);
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

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new RuntimeException("Current password is required");
        }

        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
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
        }

        AdminUser saved = adminRepository.save(admin);
        sessions.put(token, saved);
        return saved;
    }
}
