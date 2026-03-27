package com.example.infosys_project.integration;

import com.example.infosys_project.model.AdminUser;
import com.example.infosys_project.model.UserRole;
import com.example.infosys_project.repository.AdminRepository;
import com.example.infosys_project.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for API authorization and role-based access control
 * Tests Phase 8 requirement: API endpoint authorization (integration tests)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API Authorization Integration Tests")
class ApiAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String superadminToken;
    private String adminToken;
    private String analystToken;

    private AdminUser superadmin;
    private AdminUser admin;
    private AdminUser analyst;

    @BeforeEach
    @Transactional
    void setup() {
        // Clean up existing users
        adminRepository.deleteAll();

        // Create SUPERADMIN
        superadmin = new AdminUser();
        superadmin.setName("Super Admin");
        superadmin.setEmail("super@test.com");
        superadmin.setPassword(passwordEncoder.encode("password123"));
        superadmin.setRole(UserRole.SUPERADMIN);
        superadmin.setActive(true);
        superadmin.setEmailAlertsEnabled(true);
        superadmin.setCanBeDeleted(false);
        superadmin.setCreatedBy("SYSTEM");
        superadmin = adminRepository.save(superadmin);

        // Create ADMIN
        admin = new AdminUser();
        admin.setName("Admin User");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("password123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setEmailAlertsEnabled(true);
        admin.setCanBeDeleted(true);
        admin.setCreatedBy("SUPERADMIN");
        admin = adminRepository.save(admin);

        // Create ANALYST
        analyst = new AdminUser();
        analyst.setName("Analyst User");
        analyst.setEmail("analyst@test.com");
        analyst.setPassword(passwordEncoder.encode("password123"));
        analyst.setRole(UserRole.ANALYST);
        analyst.setActive(true);
        analyst.setEmailAlertsEnabled(false);
        analyst.setCanBeDeleted(true);
        analyst.setCreatedBy("ADMIN");
        analyst = adminRepository.save(analyst);

        // Login to get tokens
        superadminToken = authService.login("super@test.com", "password123");
        adminToken = authService.login("admin@test.com", "password123");
        analystToken = authService.login("analyst@test.com", "password123");
    }

    @AfterEach
    void cleanup() {
        if (superadminToken != null) authService.logout(superadminToken);
        if (adminToken != null) authService.logout(adminToken);
        if (analystToken != null) authService.logout(analystToken);
    }

    @Nested
    @DisplayName("POST /auth/login tests")
    class LoginTests {

        @Test
        @DisplayName("Should allow valid login")
        void shouldAllowValidLogin() throws Exception {
            Map<String, String> credentials = new HashMap<>();
            credentials.put("email", "super@test.com");
            credentials.put("password", "password123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(credentials)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.email").value("super@test.com"))
                    .andExpect(jsonPath("$.name").value("Super Admin"));
        }

        @Test
        @DisplayName("Should reject invalid credentials")
        void shouldRejectInvalidCredentials() throws Exception {
            Map<String, String> credentials = new HashMap<>();
            credentials.put("email", "super@test.com");
            credentials.put("password", "wrongpassword");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(credentials)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }
    }

    @Nested
    @DisplayName("GET /auth/me tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user with valid token")
        void shouldReturnCurrentUser() throws Exception {
            mockMvc.perform(get("/auth/me")
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("super@test.com"))
                    .andExpect(jsonPath("$.role").value("SUPERADMIN"));
        }

        @Test
        @DisplayName("Should reject request without token")
        void shouldRejectWithoutToken() throws Exception {
            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /auth/users tests - Role-based access")
    class ListUsersTests {

        @Test
        @DisplayName("SUPERADMIN can list users")
        void superadminCanListUsers() throws Exception {
            mockMvc.perform(get("/auth/users")
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("ADMIN can list users")
        void adminCanListUsers() throws Exception {
            mockMvc.perform(get("/auth/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("ANALYST cannot list users")
        void analystCannotListUsers() throws Exception {
            mockMvc.perform(get("/auth/users")
                            .header("Authorization", "Bearer " + analystToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /auth/create-user tests - User creation permissions")
    class CreateUserTests {

        @Test
        @DisplayName("SUPERADMIN can create ADMIN")
        void superadminCanCreateAdmin() throws Exception {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("name", "New Admin");
            newUser.put("email", "newadmin@test.com");
            newUser.put("password", "password123");
            newUser.put("role", "ADMIN");

            mockMvc.perform(post("/auth/create-user")
                            .header("Authorization", "Bearer " + superadminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("newadmin@test.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("SUPERADMIN can create ANALYST")
        void superadminCanCreateAnalyst() throws Exception {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("name", "New Analyst");
            newUser.put("email", "newanalyst@test.com");
            newUser.put("password", "password123");
            newUser.put("role", "ANALYST");

            mockMvc.perform(post("/auth/create-user")
                            .header("Authorization", "Bearer " + superadminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("ANALYST"));
        }

        @Test
        @DisplayName("ADMIN cannot create ADMIN")
        void adminCannotCreateAdmin() throws Exception {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("name", "New Admin");
            newUser.put("email", "newadmin2@test.com");
            newUser.put("password", "password123");
            newUser.put("role", "ADMIN");

            mockMvc.perform(post("/auth/create-user")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN can create ANALYST")
        void adminCanCreateAnalyst() throws Exception {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("name", "New Analyst 2");
            newUser.put("email", "newanalyst2@test.com");
            newUser.put("password", "password123");
            newUser.put("role", "ANALYST");

            mockMvc.perform(post("/auth/create-user")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("ANALYST"));
        }

        @Test
        @DisplayName("ANALYST cannot create any user")
        void analystCannotCreateUser() throws Exception {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("name", "New User");
            newUser.put("email", "newuser@test.com");
            newUser.put("password", "password123");
            newUser.put("role", "ANALYST");

            mockMvc.perform(post("/auth/create-user")
                            .header("Authorization", "Bearer " + analystToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("No one can create SUPERADMIN")
        void noOneCanCreateSuperadmin() throws Exception {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("name", "New Superadmin");
            newUser.put("email", "newsuperadmin@test.com");
            newUser.put("password", "password123");
            newUser.put("role", "SUPERADMIN");

            mockMvc.perform(post("/auth/create-user")
                            .header("Authorization", "Bearer " + superadminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /auth/users/{id} tests - User deletion permissions")
    class DeleteUserTests {

        @Test
        @DisplayName("SUPERADMIN can delete ADMIN")
        void superadminCanDeleteAdmin() throws Exception {
            mockMvc.perform(delete("/auth/users/" + admin.getId())
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SUPERADMIN can delete ANALYST")
        void superadminCanDeleteAnalyst() throws Exception {
            mockMvc.perform(delete("/auth/users/" + analyst.getId())
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN can delete ANALYST")
        void adminCanDeleteAnalyst() throws Exception {
            mockMvc.perform(delete("/auth/users/" + analyst.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN cannot delete ADMIN")
        void adminCannotDeleteAdmin() throws Exception {
            // Create another admin to test
            AdminUser anotherAdmin = new AdminUser();
            anotherAdmin.setName("Another Admin");
            anotherAdmin.setEmail("another@test.com");
            anotherAdmin.setPassword(passwordEncoder.encode("password"));
            anotherAdmin.setRole(UserRole.ADMIN);
            anotherAdmin.setActive(true);
            anotherAdmin.setCanBeDeleted(true);
            anotherAdmin = adminRepository.save(anotherAdmin);

            mockMvc.perform(delete("/auth/users/" + anotherAdmin.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ANALYST cannot delete anyone")
        void analystCannotDeleteAnyone() throws Exception {
            mockMvc.perform(delete("/auth/users/" + admin.getId())
                            .header("Authorization", "Bearer " + analystToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Cannot delete SUPERADMIN")
        void cannotDeleteSuperadmin() throws Exception {
            mockMvc.perform(delete("/auth/users/" + superadmin.getId())
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /transaction endpoints - Data access tests")
    class TransactionEndpointsTests {

        @Test
        @DisplayName("SUPERADMIN can access transactions")
        void superadminCanAccessTransactions() throws Exception {
            mockMvc.perform(get("/transaction/all")
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN can access transactions")
        void adminCanAccessTransactions() throws Exception {
            mockMvc.perform(get("/transaction/all")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ANALYST can access transactions (read-only)")
        void analystCanAccessTransactions() throws Exception {
            mockMvc.perform(get("/transaction/all")
                            .header("Authorization", "Bearer " + analystToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Unauthorized request is rejected")
        void unauthorizedRequestIsRejected() throws Exception {
            mockMvc.perform(get("/transaction/all"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /transaction/validate - Write access tests")
    class SubmitTransactionTests {

        @Test
        @DisplayName("SUPERADMIN can submit transactions")
        void superadminCanSubmitTransactions() throws Exception {
            Map<String, Object> transaction = createSampleTransaction();

            mockMvc.perform(post("/transaction/validate")
                            .header("Authorization", "Bearer " + superadminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transaction)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN can submit transactions")
        void adminCanSubmitTransactions() throws Exception {
            Map<String, Object> transaction = createSampleTransaction();

            mockMvc.perform(post("/transaction/validate")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transaction)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ANALYST cannot submit transactions (read-only)")
        void analystCannotSubmitTransactions() throws Exception {
            Map<String, Object> transaction = createSampleTransaction();

            mockMvc.perform(post("/transaction/validate")
                            .header("Authorization", "Bearer " + analystToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transaction)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /transaction/export-csv - Export permissions")
    class ExportTests {

        @Test
        @DisplayName("SUPERADMIN can export data")
        void superadminCanExport() throws Exception {
            mockMvc.perform(get("/transaction/export-csv")
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN can export data")
        void adminCanExport() throws Exception {
            mockMvc.perform(get("/transaction/export-csv")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ANALYST can export data")
        void analystCanExport() throws Exception {
            mockMvc.perform(get("/transaction/export-csv")
                            .header("Authorization", "Bearer " + analystToken))
                    .andExpect(status().isOk());
        }
    }

    // Helper method to create sample transaction data
    private Map<String, Object> createSampleTransaction() {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("accountNumber", "1234567890");
        transaction.put("amount", 5000.0);
        transaction.put("balance", 50000.0);
        transaction.put("type", "debit");
        transaction.put("merchantCategory", "retail");
        transaction.put("transactionMode", "online");
        transaction.put("location", "Mumbai");
        transaction.put("ipAddress", "192.168.1.1");
        transaction.put("deviceId", "device123");
        return transaction;
    }
}
