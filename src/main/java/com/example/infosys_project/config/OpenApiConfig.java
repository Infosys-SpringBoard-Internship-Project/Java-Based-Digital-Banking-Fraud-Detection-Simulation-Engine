package com.example.infosys_project.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Access documentation at: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FraudShield API")
                        .version("1.0.0")
                        .description("""
                                Digital Banking Fraud Detection and Simulation Engine API.
                                
                                ## Features
                                - Real-time transaction fraud detection
                                - ML-powered risk scoring
                                - Role-based access control (SUPERADMIN, ADMIN, ANALYST)
                                - Audit logging and compliance tracking
                                
                                ## Authentication
                                All endpoints (except login and register) require Bearer token authentication.
                                """)
                        .contact(new Contact()
                                .name("FraudShield Support")
                                .email("support@fraudshield.local"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")))
                .tags(List.of(
                        new Tag().name("Authentication").description("User authentication and session management"),
                        new Tag().name("User Management").description("User CRUD operations (ADMIN+ only)"),
                        new Tag().name("Transactions").description("Transaction submission and fraud detection"),
                        new Tag().name("Dashboard").description("Dashboard statistics and analytics"),
                        new Tag().name("Audit").description("Audit logs and compliance tracking"),
                        new Tag().name("System").description("System health and configuration")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Session Token")
                                .description("Session token obtained from login endpoint")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
