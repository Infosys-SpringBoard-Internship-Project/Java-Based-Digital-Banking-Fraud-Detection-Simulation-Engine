package com.example.infosys_project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RateLimitService
 */
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    @DisplayName("Should allow login attempts within limit")
    void shouldAllowLoginAttemptsWithinLimit() {
        String testIp = "192.168.1.1";
        
        // First 5 attempts should be allowed
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowLoginAttempt(testIp),
                    "Attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should block login attempts exceeding limit")
    void shouldBlockLoginAttemptsExceedingLimit() {
        String testIp = "192.168.1.2";
        
        // Exhaust the 5 allowed attempts
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowLoginAttempt(testIp);
        }
        
        // 6th attempt should be blocked
        assertFalse(rateLimitService.allowLoginAttempt(testIp),
                "6th attempt should be blocked");
    }

    @Test
    @DisplayName("Should allow password reset attempts within limit")
    void shouldAllowPasswordResetAttemptsWithinLimit() {
        String testIp = "192.168.1.3";
        
        // First 3 attempts should be allowed
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimitService.allowPasswordResetAttempt(testIp),
                    "Attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should block password reset attempts exceeding limit")
    void shouldBlockPasswordResetAttemptsExceedingLimit() {
        String testIp = "192.168.1.4";
        
        // Exhaust the 3 allowed attempts
        for (int i = 0; i < 3; i++) {
            rateLimitService.allowPasswordResetAttempt(testIp);
        }
        
        // 4th attempt should be blocked
        assertFalse(rateLimitService.allowPasswordResetAttempt(testIp),
                "4th attempt should be blocked");
    }

    @Test
    @DisplayName("Should track remaining login attempts")
    void shouldTrackRemainingLoginAttempts() {
        String testIp = "192.168.1.5";
        
        // Initially should have 5 attempts
        assertEquals(5, rateLimitService.getRemainingLoginAttempts(testIp));
        
        // After one attempt, should have 4 remaining
        rateLimitService.allowLoginAttempt(testIp);
        assertEquals(4, rateLimitService.getRemainingLoginAttempts(testIp));
    }

    @Test
    @DisplayName("Should reset login limits for IP")
    void shouldResetLoginLimitsForIp() {
        String testIp = "192.168.1.6";
        
        // Exhaust all attempts
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowLoginAttempt(testIp);
        }
        
        // Should be blocked
        assertFalse(rateLimitService.allowLoginAttempt(testIp));
        
        // Reset limits
        rateLimitService.resetLoginLimits(testIp);
        
        // Should be allowed again
        assertTrue(rateLimitService.allowLoginAttempt(testIp));
    }

    @Test
    @DisplayName("Should handle different IPs independently")
    void shouldHandleDifferentIpsIndependently() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";
        
        // Exhaust attempts for IP1
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowLoginAttempt(ip1);
        }
        
        // IP1 should be blocked, IP2 should still be allowed
        assertFalse(rateLimitService.allowLoginAttempt(ip1));
        assertTrue(rateLimitService.allowLoginAttempt(ip2));
    }

    @Test
    @DisplayName("Should allow API calls within limit")
    void shouldAllowApiCallsWithinLimit() {
        String testIp = "192.168.1.7";
        
        // First 100 calls should be allowed
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimitService.allowApiCall(testIp),
                    "API call " + (i + 1) + " should be allowed");
        }
        
        // 101st call should be blocked
        assertFalse(rateLimitService.allowApiCall(testIp),
                "101st API call should be blocked");
    }

    @Test
    @DisplayName("Should clear all limits")
    void shouldClearAllLimits() {
        String testIp = "192.168.1.8";
        
        // Exhaust login attempts
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowLoginAttempt(testIp);
        }
        assertFalse(rateLimitService.allowLoginAttempt(testIp));
        
        // Clear all limits
        rateLimitService.clearAllLimits();
        
        // Should be allowed again
        assertTrue(rateLimitService.allowLoginAttempt(testIp));
    }
}
