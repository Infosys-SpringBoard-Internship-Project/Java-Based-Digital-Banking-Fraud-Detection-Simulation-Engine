package com.example.infosys_project.filter;

import com.example.infosys_project.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate limiting filter for sensitive endpoints.
 * Applies token bucket rate limiting to prevent brute force attacks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        String clientIp = getClientIp(request);

        // Apply rate limiting for sensitive endpoints
        if (isLoginEndpoint(requestUri)) {
            if (!rateLimitService.allowLoginAttempt(clientIp)) {
                sendRateLimitResponse(response, "Too many login attempts. Please try again later.", 
                        rateLimitService.getRemainingLoginAttempts(clientIp));
                return;
            }
        } else if (isPasswordResetEndpoint(requestUri)) {
            if (!rateLimitService.allowPasswordResetAttempt(clientIp)) {
                sendRateLimitResponse(response, "Too many password reset attempts. Please try again later.",
                        rateLimitService.getRemainingPasswordResetAttempts(clientIp));
                return;
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    private boolean isLoginEndpoint(String uri) {
        return uri.equals("/auth/login");
    }

    private boolean isPasswordResetEndpoint(String uri) {
        return uri.equals("/auth/forgot-password");
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for forwarded headers (behind proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message, long remaining) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("Retry-After", "900"); // 15 minutes

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", message);
        errorBody.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorBody.put("retryAfterSeconds", 900);

        objectMapper.writeValue(response.getWriter(), errorBody);
    }
}
