package com.pharmaflow.smartfeatures.controller.fraud;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckResponseDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FraudService fraudService;

    private FraudCheckResponseDto checkResponseDto;

    @BeforeEach
    void setUp() {
        checkResponseDto = new FraudCheckResponseDto();
        checkResponseDto.setId(3L);
        checkResponseDto.setUserId(10L);
        checkResponseDto.setOrderId(20L);
        checkResponseDto.setRiskScore(45.0);
        checkResponseDto.setDecision(FraudDecision.REVIEW);
        checkResponseDto.setCheckedAt(LocalDateTime.now());
    }

    @Test
    void getChecksShouldReturn200() throws Exception {
        when(fraudService.getChecks(10L, null)).thenReturn(List.of(checkResponseDto));

        mockMvc.perform(get("/api/fraud-checks").param("userId", "10").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].decision").value("REVIEW"));
    }

    @Test
    void createRuleShouldRejectInvalidWeight() throws Exception {
        mockMvc.perform(post("/api/fraud-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleName": "Late pickup",
                                  "description": "Repeated delayed pickup",
                                  "weight": 0,
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(fraudService, never()).createRule(any());
    }
}
