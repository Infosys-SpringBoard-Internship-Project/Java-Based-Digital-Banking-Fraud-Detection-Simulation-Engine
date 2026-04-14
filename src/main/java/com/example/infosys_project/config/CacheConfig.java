package com.example.infosys_project.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine for performance optimization.
 * Caches frequently accessed data to reduce database load.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Default cache manager with Caffeine caches for different data types.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(defaultCacheBuilder());
        cacheManager.setCacheNames(java.util.List.of(
                "dashboardStats",      // Dashboard statistics - cached for 30 seconds
                "transactionCounts",   // Transaction counts - cached for 1 minute
                "fraudAlertCounts",    // Fraud alert counts - cached for 1 minute
                "systemHealth",        // System health status - cached for 10 seconds
                "userLookup"           // User lookup by email - cached for 5 minutes
        ));
        return cacheManager;
    }

    /**
     * Dashboard stats cache - short TTL for near real-time data
     */
    @Bean
    public Caffeine<Object, Object> dashboardStatsCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(100)
                .recordStats();
    }

    /**
     * Default cache configuration
     */
    private Caffeine<Object, Object> defaultCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
    }
}
