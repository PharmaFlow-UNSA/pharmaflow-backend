package com.pharmaflow.userhealth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.userhealth.dto.UserCreateDTO;
import com.pharmaflow.userhealth.dto.UserDTO;
import com.pharmaflow.userhealth.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security Integration Tests for User & Health Service.
 *
 * Role matrix:
 *   GET endpoints           → any authenticated user (read)
 *   POST/PUT (users)        → DOCTOR / ADMIN (create/update users)
 *   POST/PUT (profiles)     → DOCTOR / USER / ADMIN (manage health profiles)
 *   DELETE (users)          → ADMIN only
 *   DELETE (profiles, etc)  → ADMIN or DOCTOR
 *
 * Tests JWT authentication failures: missing, malformed, wrong signature, expired tokens.
 * Tests authorization failures: insufficient permissions (403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User & Health Service — Security Integration (Zadatak 8.1)")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Value("${jwt.secret:pharmaflow-secret-key-2024-very-long-and-secure-key-for-production}")
    private String secret;

    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String generateToken(String subject, List<String> roles, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(signingKey)
                .compact();
    }

    private String validToken(String role) {
        return generateToken("test@pharmaflow.ba", List.of(role), 60_000L);
    }

    private UserCreateDTO sampleUserCreateDTO() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setEmail("test@pharmaflow.ba");
        dto.setPassword("TestPass123!");
        return dto;
    }

    private UserDTO sampleUserDTO() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setEmail("test@pharmaflow.ba");
        return dto;
    }

    // ── Unauthenticated cases (401) ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/users without token → 401")
    void getUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/users with malformed token → 401")
    void getUsers_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users with wrong-signature token → 401")
    void getUsers_withWrongSignature_returns401() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "completely-different-key-completely-different-key".getBytes());
        String foreignToken = Jwts.builder()
                .subject("attacker@evil.example")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(otherKey)
                .compact();

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users with expired token → 401")
    void getUsers_withExpiredToken_returns401() throws Exception {
        String expired = generateToken("test@pharmaflow.ba", List.of("ROLE_USER"), -1_000L);
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    // ── GET: Any authenticated user can read ─────────────────────────────────

    @Test
    @DisplayName("GET /api/users with ROLE_USER → 200")
    void getUsers_asUser_returns200() throws Exception {
        when(userService.findAll(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/users with ROLE_DOCTOR → 200")
    void getUsers_asDoctor_returns200() throws Exception {
        when(userService.findAll(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + validToken("ROLE_DOCTOR")))
                .andExpect(status().isOk());
    }

    // ── POST /api/users: Anyone can register ────────────────────────────────

    @Test
    @DisplayName("POST /api/users with ROLE_USER → 201 (anyone can create user)")
    void createUser_asUser_returns201() throws Exception {
        when(userService.createUser(any())).thenReturn(sampleUserDTO());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUserCreateDTO())))
                .andExpect(status().isCreated());
    }

    // ── PUT/PATCH /api/users: DOCTOR or ADMIN ────────────────────────────────

    @Test
    @DisplayName("PUT /api/users/{id} with ROLE_USER → 403")
    void updateUser_asUser_returns403() throws Exception {
        mockMvc.perform(put("/api/users/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUserCreateDTO())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} with ROLE_DOCTOR → 200")
    void updateUser_asDoctor_returns200() throws Exception {
        when(userService.updateUser(eq(1L), any())).thenReturn(sampleUserDTO());

        mockMvc.perform(put("/api/users/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUserCreateDTO())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/users/{id} with ROLE_ADMIN → 200")
    void patchUser_asAdmin_returns200() throws Exception {
        when(userService.patchUser(eq(1L), any())).thenReturn(sampleUserDTO());

        mockMvc.perform(patch("/api/users/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_ADMIN"))
                        .contentType("application/json")
                        .content("[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"Updated\"}]"))
                .andExpect(status().isOk());
    }

    // ── DELETE /api/users: ADMIN only ────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/users/{id} with ROLE_DOCTOR → 403")
    void deleteUser_asDoctor_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} with ROLE_PHARMACIST → 403")
    void deleteUser_asPharmacist_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_PHARMACIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} with ROLE_ADMIN → 204")
    void deleteUser_asAdmin_returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_ADMIN")))
                .andExpect(status().isNoContent());
    }

    // ── Patient Profiles: DOCTOR or USER can write, ADMIN can do everything ──

    @Test
    @DisplayName("POST /api/patient-profiles with ROLE_PHARMACIST → 403")
    void createPatientProfile_asPharmacist_returns403() throws Exception {
        mockMvc.perform(post("/api/patient-profiles")
                        .header("Authorization", "Bearer " + validToken("ROLE_PHARMACIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":70.0,\"height\":175.0,\"bloodType\":\"O_POSITIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/patient-profiles with ROLE_DOCTOR → 201")
    void createPatientProfile_asDoctor_returns201() throws Exception {
        mockMvc.perform(post("/api/patient-profiles")
                        .header("Authorization", "Bearer " + validToken("ROLE_DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":70.0,\"height\":175.0,\"bloodType\":\"O_POSITIVE\"}"))
                .andExpect(status().isCreated());
    }

    // ── Open paths (kept permitAll) ──────────────────────────────────────────


    @Test
    @DisplayName("POST /api/auth/register is public")
    void authRegister_isPublic() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"test@example.com\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isCreated());
    }
}

