package com.pharmaflow.smartfeatures.controller.symptom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.service.symptom.SymptomProductMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SymptomProductMatchController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class SymptomProductMatchControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private SymptomProductMatchService symptomProductMatchService;

  private SymptomProductMatchResponseDto responseDto;

  @BeforeEach
  void setUp() {
    responseDto = new SymptomProductMatchResponseDto();
    responseDto.setId(1L);
    responseDto.setSymptomId(5L);
    responseDto.setProductId(100L);
    responseDto.setRelevanceScore(0.9);
    responseDto.setMatchReason("Helpful for cough");
  }

  @Test
  void createMatchShouldReturn201() throws Exception {
    when(symptomProductMatchService.createMatch(any(), any())).thenReturn(responseDto);

    mockMvc
        .perform(
            post("/api/symptoms/5/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "productId": 100,
                                  "relevanceScore": 0.9,
                                  "matchReason": "Helpful for cough"
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.symptomId").value(5));
  }

  @Test
  void createMatchShouldRejectInvalidPathVariable() throws Exception {
    mockMvc
        .perform(
            post("/api/symptoms/-1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "productId": 100,
                                  "relevanceScore": 0.9
                                }
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSTRAINT_VIOLATION"));

    verify(symptomProductMatchService, never()).createMatch(any(), any());
  }
}
