package com.pharmaflow.smartfeatures.controller.fraud;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudLogResponseDto;
import com.pharmaflow.smartfeatures.dto.fraud.FraudRuleResponseDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.service.fraud.FraudService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FraudController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class FraudControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FraudService fraudService;

  private FraudRuleResponseDto ruleResponse;
  private FraudCheckResponseDto checkResponse;
  private FraudLogResponseDto logResponse;

  @BeforeEach
  void setUp() {
    ruleResponse =
        new FraudRuleResponseDto(
            1L, "High value order", "HIGH_VALUE_ORDER", "ORDER", "Large order total", 25.0, true);
    checkResponse =
        new FraudCheckResponseDto(
            2L, 10L, 100L, 72.5, FraudDecision.BLOCKED, LocalDateTime.of(2026, 5, 5, 8, 0));
    logResponse =
        new FraudLogResponseDto(
            3L,
            2L,
            1L,
            FraudEventType.TRIGGERED,
            "Rule triggered",
            25.0,
            LocalDateTime.of(2026, 5, 5, 8, 1));
  }

  @Test
  void getRulesShouldReturn200AndList() throws Exception {
    when(fraudService.getRules()).thenReturn(List.of(ruleResponse));

    mockMvc
        .perform(get("/api/fraud-rules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].ruleCode").value("HIGH_VALUE_ORDER"));
  }

  @Test
  void getRuleShouldReturn200WhenFound() throws Exception {
    when(fraudService.getRule(1L)).thenReturn(ruleResponse);

    mockMvc
        .perform(get("/api/fraud-rules/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ruleName").value("High value order"));
  }

  @Test
  void getRuleShouldReturn404WhenMissing() throws Exception {
    when(fraudService.getRule(404L))
        .thenThrow(new ResourceNotFoundException("Fraud rule not found with id: 404"));

    mockMvc
        .perform(get("/api/fraud-rules/404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void createRuleShouldReturn201WhenValid() throws Exception {
    when(fraudService.createRule(any())).thenReturn(ruleResponse);

    mockMvc
        .perform(
            post("/api/fraud-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson("High value order", "HIGH_VALUE_ORDER", "ORDER", 25.0, true)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @Test
  void createRuleShouldReturn400WhenInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/fraud-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson("", "HV", "OR", 0.0, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(fraudService, never()).createRule(any());
  }

  @Test
  void updateRuleShouldReturn200WhenValid() throws Exception {
    when(fraudService.updateRule(eq(1L), any())).thenReturn(ruleResponse);

    mockMvc
        .perform(
            put("/api/fraud-rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson("High value order", "HIGH_VALUE_ORDER", "ORDER", 25.0, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ruleCode").value("HIGH_VALUE_ORDER"));
  }

  @Test
  void updateRuleShouldReturn404WhenMissing() throws Exception {
    when(fraudService.updateRule(eq(404L), any()))
        .thenThrow(new ResourceNotFoundException("Fraud rule not found with id: 404"));

    mockMvc
        .perform(
            put("/api/fraud-rules/404")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson("High value order", "HIGH_VALUE_ORDER", "ORDER", 25.0, true)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void updateRuleShouldReturn400WhenInvalid() throws Exception {
    mockMvc
        .perform(
            put("/api/fraud-rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleJson("", "HV", "OR", 0.0, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(fraudService, never()).updateRule(any(), any());
  }

  @Test
  void deleteRuleShouldReturn204WhenFound() throws Exception {
    mockMvc.perform(delete("/api/fraud-rules/1")).andExpect(status().isNoContent());

    verify(fraudService).deleteRule(1L);
  }

  @Test
  void deleteRuleShouldReturn404WhenMissing() throws Exception {
    doThrow(new ResourceNotFoundException("Fraud rule not found with id: 404"))
        .when(fraudService)
        .deleteRule(404L);

    mockMvc
        .perform(delete("/api/fraud-rules/404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void getRuleLogsShouldReturn200WhenRuleExists() throws Exception {
    when(fraudService.getLogsByRule(1L)).thenReturn(List.of(logResponse));

    mockMvc
        .perform(get("/api/fraud-rules/1/logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fraudRuleId").value(1))
        .andExpect(jsonPath("$[0].eventType").value("TRIGGERED"));
  }

  @Test
  void getRuleLogsShouldReturn404WhenRuleMissing() throws Exception {
    when(fraudService.getLogsByRule(404L))
        .thenThrow(new ResourceNotFoundException("Fraud rule not found with id: 404"));

    mockMvc
        .perform(get("/api/fraud-rules/404/logs"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void createCheckShouldReturn201WhenValid() throws Exception {
    when(fraudService.createCheck(any())).thenReturn(checkResponse);

    mockMvc
        .perform(
            post("/api/fraud-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"orderId": 100}
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.decision").value("BLOCKED"));
  }

  @Test
  void createCheckShouldReturn400WhenInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/fraud-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"orderId": -1}
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(fraudService, never()).createCheck(any());
  }

  @Test
  void getCheckShouldReturn200WhenFound() throws Exception {
    when(fraudService.getCheck(2L)).thenReturn(checkResponse);

    mockMvc
        .perform(get("/api/fraud-checks/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value(100))
        .andExpect(jsonPath("$.riskScore").value(72.5));
  }

  @Test
  void getCheckShouldReturn404WhenMissing() throws Exception {
    when(fraudService.getCheck(404L))
        .thenThrow(new ResourceNotFoundException("Fraud check not found with id: 404"));

    mockMvc
        .perform(get("/api/fraud-checks/404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void getChecksShouldReturn200WithOptionalFilters() throws Exception {
    when(fraudService.getChecks(10L, 100L)).thenReturn(List.of(checkResponse));

    mockMvc
        .perform(get("/api/fraud-checks").param("userId", "10").param("orderId", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].userId").value(10))
        .andExpect(jsonPath("$[0].orderId").value(100));
  }

  @Test
  void getChecksShouldReturn400WhenFilterIsInvalid() throws Exception {
    mockMvc
        .perform(get("/api/fraud-checks").param("userId", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSTRAINT_VIOLATION"));

    verify(fraudService, never()).getChecks(any(), any());
  }

  @Test
  void getCheckLogsShouldReturn200WhenCheckExists() throws Exception {
    when(fraudService.getLogsByCheck(2L)).thenReturn(List.of(logResponse));

    mockMvc
        .perform(get("/api/fraud-checks/2/logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fraudCheckId").value(2))
        .andExpect(jsonPath("$[0].scoreContribution").value(25.0));
  }

  @Test
  void getCheckLogsShouldReturn404WhenCheckMissing() throws Exception {
    when(fraudService.getLogsByCheck(404L))
        .thenThrow(new ResourceNotFoundException("Fraud check not found with id: 404"));

    mockMvc
        .perform(get("/api/fraud-checks/404/logs"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  private String ruleJson(
      String ruleName, String ruleCode, String category, Double weight, Boolean active) {
    return """
                {
                  "ruleName": %s,
                  "ruleCode": %s,
                  "category": %s,
                  "description": "Large order total",
                  "weight": %s,
                  "isActive": %s
                }
                """
        .formatted(jsonValue(ruleName), jsonValue(ruleCode), jsonValue(category), weight, active);
  }

  private String jsonValue(String value) {
    return value == null ? "null" : "\"" + value + "\"";
  }
}
