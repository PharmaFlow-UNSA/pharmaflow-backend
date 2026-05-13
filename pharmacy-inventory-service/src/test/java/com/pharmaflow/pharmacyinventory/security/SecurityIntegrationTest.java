package com.pharmaflow.pharmacyinventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.PharmacyCreateDTO;
import com.pharmaflow.pharmacyinventory.dto.PharmacyDTO;
import com.pharmaflow.pharmacyinventory.service.PharmacyService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks the JWT + RBAC filter chain end-to-end:
 *   1. No token       → 401
 *   2. Bad token      → 401
 *   3. Expired token  → 401
 *   4. Wrong role     → 403
 *   5. Right role     → 200/201
 *   6. Admin override → 201
 *
 * This is the Zadatak 8.1 evidence that authorization is wired correctly at
 * the service level, not only at the gateway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Pharmacy Inventory Service — Security Integration (Zadatak 8.1)")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PharmacyService pharmacyService;

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

    private PharmacyCreateDTO sampleCreateDto() {
        PharmacyCreateDTO dto = new PharmacyCreateDTO();
        dto.setName("Apoteka Centar");
        dto.setAddress("Marsala Tita 10");
        dto.setCity("Sarajevo");
        dto.setPhoneNumber("+387-33-123456");
        dto.setEmail("centar@pharmaflow.ba");
        dto.setOpeningHours("08:00-20:00");
        return dto;
    }

    private PharmacyDTO sampleResponseDto() {
        PharmacyDTO dto = new PharmacyDTO();
        dto.setId(1L);
        dto.setName("Apoteka Centar");
        dto.setAddress("Marsala Tita 10");
        dto.setCity("Sarajevo");
        dto.setPhoneNumber("+387-33-123456");
        dto.setEmail("centar@pharmaflow.ba");
        dto.setOpeningHours("08:00-20:00");
        return dto;
    }

    // ── Unauthenticated cases ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/pharmacies without token → 401 with JSON body")
    void getPharmacies_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/pharmacies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/pharmacies with malformed token → 401")
    void getPharmacies_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/pharmacies")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/pharmacies with wrong-signature token → 401")
    void getPharmacies_withWrongSignature_returns401() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "completely-different-key-completely-different-key".getBytes());
        String foreignToken = Jwts.builder()
                .subject("attacker@evil.example")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(otherKey)
                .compact();

        mockMvc.perform(get("/api/pharmacies")
                        .header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/pharmacies with expired token → 401")
    void getPharmacies_withExpiredToken_returns401() throws Exception {
        String expired = generateToken("test-user@pharmaflow.ba", List.of("ROLE_USER"), -1_000L);
        mockMvc.perform(get("/api/pharmacies")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    // ── Authorization cases ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/pharmacies with ROLE_USER → 200 (read is allowed for any authenticated user)")
    void getPharmacies_asUser_returns200() throws Exception {
        when(pharmacyService.findAll(any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/pharmacies")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/pharmacies with ROLE_USER → 403 (write needs PHARMACIST/ADMIN)")
    void createPharmacy_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/pharmacies")
                        .header("Authorization", "Bearer " + validToken("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateDto())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/pharmacies with ROLE_DOCTOR → 403 (write needs PHARMACIST/ADMIN)")
    void createPharmacy_asDoctor_returns403() throws Exception {
        mockMvc.perform(post("/api/pharmacies")
                        .header("Authorization", "Bearer " + validToken("ROLE_DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/pharmacies with ROLE_PHARMACIST → 201")
    void createPharmacy_asPharmacist_returns201() throws Exception {
        when(pharmacyService.createPharmacy(any())).thenReturn(sampleResponseDto());

        mockMvc.perform(post("/api/pharmacies")
                        .header("Authorization", "Bearer " + validToken("ROLE_PHARMACIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/pharmacies with ROLE_ADMIN → 201 (admin bypasses fine-grained checks)")
    void createPharmacy_asAdmin_returns201() throws Exception {
        when(pharmacyService.createPharmacy(any())).thenReturn(sampleResponseDto());

        mockMvc.perform(post("/api/pharmacies")
                        .header("Authorization", "Bearer " + validToken("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateDto())))
                .andExpect(status().isCreated());
    }

    // ── Open paths (kept permitAll for now) ─────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health is public — no auth required")
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
