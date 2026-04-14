package com.example.infosys_project.dto;

import jakarta.validation.constraints.Email;

/**
 * Password reset request DTO with validation
 */
public record PasswordResetRequest(
    @Email(message = "Invalid email format")
    String email,

    String role
) {}
