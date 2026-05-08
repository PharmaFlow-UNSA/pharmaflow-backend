package com.pharmaflow.smartfeatures.controller.symptom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchResponseDto;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.service.symptom.SymptomSearchService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SymptomSearchController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class SymptomSearchControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SymptomSearchService symptomSearchService;

  private SymptomSearchResponseDto responseDto;

  @BeforeEach
  void setUp() {
    responseDto = new SymptomSearchResponseDto();
    responseDto.setId(1L);
    responseDto.setUserId(10L);
    responseDto.setPatientProfileId(20L);
    responseDto.setSearchQuery("dry cough");
    responseDto.setSearchedAt(LocalDateTime.now());
  }

  @Test
  void createSearchShouldReturn201() throws Exception {
    when(symptomSearchService.createSearch(any())).thenReturn(responseDto);

    mockMvc
        .perform(
            post("/api/symptom-searches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userId": 10,
                                  "patientProfileId": 20,
                                  "searchQuery": "dry cough"
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.searchQuery").value("dry cough"));
  }

  @Test
  void addItemShouldRejectMissingSymptomId() throws Exception {
    mockMvc
        .perform(
            post("/api/symptom-searches/1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(symptomSearchService, never()).addItem(any(), any());
  }
}
