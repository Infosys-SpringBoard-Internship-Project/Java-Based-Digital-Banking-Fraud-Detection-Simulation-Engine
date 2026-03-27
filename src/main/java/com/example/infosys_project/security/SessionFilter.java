package com.example.infosys_project.security;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.UserRole;
import com.example.infosys_project.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SessionFilter extends OncePerRequestFilter {

    @Autowired
    private AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            token = extractCookieToken(request.getCookies());
        }

        Optional<AdminUser> adminOpt = authService.getAdminFromToken(token);
        if (adminOpt.isEmpty()) {
            if (isHtmlPageRequest(request, path)) {
                response.sendRedirect("/pages/login.html");
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            }
            return;
        }

        AdminUser admin = adminOpt.get();

        if (!hasPageAccess(admin.getRole(), path)) {
            if (isHtmlPageRequest(request, path)) {
                response.sendRedirect("/pages/dashboard.html");
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            }
            return;
        }

        if (!hasApiAccess(admin.getRole(), request.getMethod(), path)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Read-only access");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.equals("/auth")
                || path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.equals("/auth/bootstrap-status")
                || path.equals("/auth/forgot-password")
                || path.equals("/pages/login.html")
                || path.equals("/pages/admin-login.html")
                || path.equals("/pages/superadmin-setup.html")
                || path.equals("/pages/admin-register.html")
                || path.equals("/pages/index.html")
                || path.equals("/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/assets/")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".svg")
                || path.endsWith(".gif")
                || path.endsWith(".ico")
                || path.endsWith(".woff")
                || path.endsWith(".woff2")
                || path.startsWith("/actuator")
                || path.startsWith("/error")
                || path.equals("/favicon.ico");
    }

    private boolean hasPageAccess(UserRole role, String path) {
        if (path.equals("/pages/user-management.html")) {
            return RoleChecker.canManageUsers(role);
        }
        if (path.equals("/pages/dashboard-simulation.html")) {
            return RoleChecker.canAccessSimulation(role);
        }
        if (path.equals("/pages/dashboard-manual.html")) {
            return RoleChecker.hasWriteAccess(role);
        }
        return true;
    }

    private boolean hasApiAccess(UserRole role, String method, String path) {
        if (role != UserRole.ANALYST) {
            return true;
        }

        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return !path.equals("/transaction/generate") && !path.equals("/transaction/autoValidate");
        }

        return path.equals("/auth/logout")
                || path.equals("/auth/update-credentials")
                || path.equals("/auth/toggle-alerts");
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private String extractCookieToken(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("fraud_session".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isHtmlPageRequest(HttpServletRequest request, String path) {
        String accept = request.getHeader("Accept");
        boolean wantsHtml = accept != null && accept.contains("text/html");
        return "GET".equalsIgnoreCase(request.getMethod()) && (path.endsWith(".html") || wantsHtml);
    }
}
