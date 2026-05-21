package com.pharmaflow.smartfeatures.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleResponseDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.service.fraud.FraudService;
import com.pharmaflow.smartfeatures.service.symptom.SymptomService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmartFeaturesSecurityTest {

  private static final String SECRET =
      "pharmaflow-secret-key-2024-very-long-and-secure-key-for-production";

  @Autowired private MockMvc mockMvc;

  @MockBean private SymptomService symptomService;

  @MockBean private FraudService fraudService;

  @Test
  void businessEndpointWithoutTokenReturns401() throws Exception {
    mockMvc.perform(get("/api/symptoms")).andExpect(status().isUnauthorized());
  }

  @Test
  void businessEndpointWithInvalidTokenReturns401() throws Exception {
    mockMvc
        .perform(get("/api/symptoms").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void publicActuatorAndSwaggerEndpointsWorkWithoutToken() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  @Test
  void userCanReadSymptoms() throws Exception {
    when(symptomService.getAllSymptoms()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/symptoms").header("Authorization", bearer("ROLE_USER")))
        .andExpect(status().isOk());
  }

  @Test
  void userCannotCreateFraudRule() throws Exception {
    mockMvc
        .perform(
            post("/api/fraud-rules")
                .header("Authorization", bearer("ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(fraudRuleJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanCreateFraudRule() throws Exception {
    when(fraudService.createRule(any()))
        .thenReturn(
            new FraudRuleResponseDto(
                1L, "Zadatak 8.1 security rule", "ZADATAK_8_1_SECURITY", "ORDER", null, 10.0, true));

    mockMvc
        .perform(
            post("/api/fraud-rules")
                .header("Authorization", bearer("ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(fraudRuleJson()))
        .andExpect(status().isCreated());
  }

  @Test
  void pharmacistCanAccessFraudCheckEndpoint() throws Exception {
    when(fraudService.createCheck(any()))
        .thenReturn(
            new FraudCheckResponseDto(
                1L, 10L, 100L, 0.0, FraudDecision.APPROVED, LocalDateTime.now()));

    MvcResult result =
        mockMvc
            .perform(
                post("/api/fraud-checks")
                    .header("Authorization", bearer("ROLE_PHARMACIST"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"orderId\":100}"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
  }

  private String bearer(String role) {
    return "Bearer " + token(role);
  }

  private String token(String role) {
    return Jwts.builder()
        .subject("security-test@example.com")
        .claim("roles", List.of(role))
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusSeconds(3600)))
        .signWith(signingKey())
        .compact();
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }

  private String fraudRuleJson() {
    return """
        {
          "ruleName": "Zadatak 8.1 security rule",
          "ruleCode": "ZADATAK_8_1_SECURITY",
          "category": "ORDER",
          "description": "Created by security test",
          "weight": 10.0,
          "isActive": true
        }
        """;
  }
}
