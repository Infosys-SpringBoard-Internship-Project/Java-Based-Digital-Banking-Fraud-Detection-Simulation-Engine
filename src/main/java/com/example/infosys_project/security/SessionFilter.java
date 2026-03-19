package com.example.infosys_project.security;

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

@Component
public class SessionFilter extends OncePerRequestFilter {

    @Autowired
    private AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            token = extractCookieToken(request.getCookies());
        }

        if (authService.getAdminFromToken(token).isEmpty()) {
            if (isHtmlPageRequest(request, path)) {
                response.sendRedirect("/pages/admin-login.html");
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        return path.startsWith("/auth/")
                || path.equals("/auth")
                || path.equals("/pages/admin-login.html")
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
