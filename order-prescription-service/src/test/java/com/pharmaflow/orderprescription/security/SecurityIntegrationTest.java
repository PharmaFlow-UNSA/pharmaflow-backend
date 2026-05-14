package com.pharmaflow.orderprescription.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.PrescriptionCreateDTO;
import com.pharmaflow.orderprescription.dto.PrescriptionDTO;
import com.pharmaflow.orderprescription.service.PrescriptionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks the JWT + RBAC filter chain end-to-end for order-prescription-service.
 *
 * Role matrix exercised:
 *   GET    /api/prescriptions       → any authenticated user
 *   POST   /api/prescriptions       → any authenticated user (patient uploads recept)
 *   PATCH  /api/prescriptions/{id}  → DOCTOR / PHARMACIST / ADMIN  (approve / reject)
 *   DELETE /api/prescriptions/{id}  → ADMIN only
 *
 * Plus the standard JWT failure cases: missing / malformed / wrong-signature / expired token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Order Prescription Service — Security Integration (Zadatak 8.1)")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrescriptionService prescriptionService;

    @Value("${jwt.secret}")
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
        return generateToken("test-user@pharmaflow.ba", List.of(role), 60_000L);
    }

    private PrescriptionCreateDTO sampleCreateDto() {
        return new PrescriptionCreateDTO(42L, "https://example.com/recept.png");
    }

    private PrescriptionDTO samplePrescriptionDto() {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setId(1L);
        dto.setUserId(42L);
        dto.setImageUrl("https://example.com/recept.png");
        dto.setStatus("PENDING");
        return dto;
    }

    private String approvedPatch() {
        return "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"APPROVED\"}]";
    }

    // ── Unauthenticated cases ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/prescriptions without token → 401")
    void getPrescriptions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/prescriptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/prescriptions with malformed token → 401")
    void getPrescriptions_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/prescriptions")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/prescriptions with wrong-signature token → 401")
    void getPrescriptions_withWrongSignature_returns401() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "completely-different-key-completely-different-key".getBytes());
        String foreignToken = Jwts.builder()
                .subject("attacker@evil.example")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(otherKey)
                .compact();

        mockMvc.perform(get("/api/prescriptions")
                        .header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/prescriptions with expired token → 401")
    void getPrescriptions_withExpiredToken_returns401() throws Exception {
        String expired = generateToken("test-user@pharmaflow.ba", List.of("ROLE_USER"), -1_000L);
        mockMvc.perform(get("/api/prescriptions")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    // ── GET reads are open to any authenticated user ────────────────────────

    @Test
    @DisplayName("GET /api/prescriptions with ROLE_USER → 200")
    void getPrescriptions_asUser_returns200() throws Exception {
        when(prescriptionService.findAll(any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/prescriptions")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER")))
                .andExpect(status().isOk());
    }

    // ── POST: any authenticated user (patient uploads their own recept) ────

    @Test
    @DisplayName("POST /api/prescriptions with ROLE_USER → 201 (patient uploading recept)")
    void createPrescription_asUser_returns201() throws Exception {
        when(prescriptionService.createPrescription(any())).thenReturn(samplePrescriptionDto());

        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ── PATCH (approve / reject): DOCTOR / PHARMACIST / ADMIN ───────────────

    @Test
    @DisplayName("PATCH /api/prescriptions/{id} with ROLE_USER → 403")
    void patchPrescription_asUser_returns403() throws Exception {
        mockMvc.perform(patch("/api/prescriptions/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER"))
                        .contentType("application/json")
                        .content(approvedPatch()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("PATCH /api/prescriptions/{id} with ROLE_DOCTOR → 200 (approve)")
    void patchPrescription_asDoctor_returns200() throws Exception {
        when(prescriptionService.patchPrescription(eq(1L), any())).thenReturn(samplePrescriptionDto());

        mockMvc.perform(patch("/api/prescriptions/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_DOCTOR"))
                        .contentType("application/json")
                        .content(approvedPatch()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/prescriptions/{id} with ROLE_PHARMACIST → 200")
    void patchPrescription_asPharmacist_returns200() throws Exception {
        when(prescriptionService.patchPrescription(eq(1L), any())).thenReturn(samplePrescriptionDto());

        mockMvc.perform(patch("/api/prescriptions/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_PHARMACIST"))
                        .contentType("application/json")
                        .content(approvedPatch()))
                .andExpect(status().isOk());
    }

    // ── DELETE: ADMIN only ──────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/prescriptions/{id} with ROLE_DOCTOR → 403 (only ADMIN can delete)")
    void deletePrescription_asDoctor_returns403() throws Exception {
        mockMvc.perform(delete("/api/prescriptions/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/prescriptions/{id} with ROLE_PHARMACIST → 403")
    void deletePrescription_asPharmacist_returns403() throws Exception {
        mockMvc.perform(delete("/api/prescriptions/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_PHARMACIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/prescriptions/{id} with ROLE_ADMIN → 204")
    void deletePrescription_asAdmin_returns204() throws Exception {
        doNothing().when(prescriptionService).deletePrescription(1L);

        mockMvc.perform(delete("/api/prescriptions/1")
                        .header("Authorization", "Bearer " + validToken("ROLE_ADMIN")))
                .andExpect(status().isNoContent());
    }

    // ── Open paths (kept permitAll for now) ─────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health is public — no auth required")
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
