package com.example.infosys_project.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using token bucket algorithm.
 * Protects sensitive endpoints from brute force attacks.
 */
@Service
public class RateLimitService {

    // Rate limiters per IP address for different endpoint types
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    // Configuration: Login attempts
    private static final int LOGIN_CAPACITY = 5;           // 5 attempts
    private static final Duration LOGIN_REFILL = Duration.ofMinutes(15); // refill every 15 minutes

    // Configuration: Password reset attempts
    private static final int RESET_CAPACITY = 3;           // 3 attempts
    private static final Duration RESET_REFILL = Duration.ofHours(1); // refill every hour

    // Configuration: General API calls
    private static final int API_CAPACITY = 100;           // 100 requests
    private static final Duration API_REFILL = Duration.ofMinutes(1); // refill every minute

    /**
     * Check if a login attempt is allowed for the given IP
     * @param ipAddress client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean allowLoginAttempt(String ipAddress) {
        Bucket bucket = loginBuckets.computeIfAbsent(ipAddress, this::createLoginBucket);
        return bucket.tryConsume(1);
    }

    /**
     * Check if a password reset attempt is allowed for the given IP
     * @param ipAddress client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean allowPasswordResetAttempt(String ipAddress) {
        Bucket bucket = passwordResetBuckets.computeIfAbsent(ipAddress, this::createPasswordResetBucket);
        return bucket.tryConsume(1);
    }

    /**
     * Check if a general API call is allowed for the given IP
     * @param ipAddress client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean allowApiCall(String ipAddress) {
        Bucket bucket = apiBuckets.computeIfAbsent(ipAddress, this::createApiBucket);
        return bucket.tryConsume(1);
    }

    /**
     * Get remaining login attempts for an IP
     * @param ipAddress client IP address
     * @return remaining attempts
     */
    public long getRemainingLoginAttempts(String ipAddress) {
        Bucket bucket = loginBuckets.get(ipAddress);
        return bucket != null ? bucket.getAvailableTokens() : LOGIN_CAPACITY;
    }

    /**
     * Get remaining password reset attempts for an IP
     * @param ipAddress client IP address
     * @return remaining attempts
     */
    public long getRemainingPasswordResetAttempts(String ipAddress) {
        Bucket bucket = passwordResetBuckets.get(ipAddress);
        return bucket != null ? bucket.getAvailableTokens() : RESET_CAPACITY;
    }

    /**
     * Reset rate limits for a specific IP (e.g., after successful login)
     * @param ipAddress client IP address
     */
    public void resetLoginLimits(String ipAddress) {
        loginBuckets.remove(ipAddress);
    }

    /**
     * Clear all rate limit buckets (for testing or admin purposes)
     */
    public void clearAllLimits() {
        loginBuckets.clear();
        passwordResetBuckets.clear();
        apiBuckets.clear();
    }

    private Bucket createLoginBucket(String key) {
        Bandwidth limit = Bandwidth.classic(LOGIN_CAPACITY, Refill.greedy(LOGIN_CAPACITY, LOGIN_REFILL));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createPasswordResetBucket(String key) {
        Bandwidth limit = Bandwidth.classic(RESET_CAPACITY, Refill.greedy(RESET_CAPACITY, RESET_REFILL));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createApiBucket(String key) {
        Bandwidth limit = Bandwidth.classic(API_CAPACITY, Refill.greedy(API_CAPACITY, API_REFILL));
        return Bucket.builder().addLimit(limit).build();
    }
}
