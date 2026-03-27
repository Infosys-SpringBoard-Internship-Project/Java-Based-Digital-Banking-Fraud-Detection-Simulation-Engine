package com.example.infosys_project.interceptor;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.ApiLog;
import com.example.infosys_project.repository.ApiLogRepository;
import com.example.infosys_project.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ApiLogInterceptor implements HandlerInterceptor {

    private final ApiLogRepository apiLogRepository;
    private final AuthService authService;

    public ApiLogInterceptor(ApiLogRepository apiLogRepository, AuthService authService) {
        this.apiLogRepository = apiLogRepository;
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("apiLogStartTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String path = request.getRequestURI();
        if (skipPath(path)) {
            return;
        }

        Object startedAt = request.getAttribute("apiLogStartTime");
        long responseTime = startedAt instanceof Long ? System.currentTimeMillis() - (Long) startedAt : 0L;

        ApiLog apiLog = new ApiLog();
        apiLog.setTimestamp(LocalDateTime.now());
        apiLog.setEndpoint(path);
        apiLog.setMethod(request.getMethod());
        apiLog.setStatusCode(response.getStatus());
        apiLog.setResponseTimeMs(responseTime);
        apiLog.setIpAddress(request.getRemoteAddr());
        apiLog.setUserEmail(resolveUserEmail(request));
        if (ex != null) {
            apiLog.setErrorMessage(ex.getMessage());
        }
        apiLogRepository.save(apiLog);
    }

    private boolean skipPath(String path) {
        return path == null
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".svg")
                || path.startsWith("/favicon")
                || path.startsWith("/error");
    }

    private String resolveUserEmail(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7).trim();
        }
        if ((token == null || token.isBlank()) && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("fraud_session".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        Optional<AdminUser> admin = authService.getAdminFromToken(token);
        return admin.map(AdminUser::getEmail).orElse("ANONYMOUS");
    }
}
