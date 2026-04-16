package com.pharmaflow.smartfeatures.controller.recommendation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.service.recommendation.RecommendationService;
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

@WebMvcTest(RecommendationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    private RecommendationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        responseDto = new RecommendationResponseDto();
        responseDto.setId(1L);
        responseDto.setUserId(10L);
        responseDto.setPatientProfileId(11L);
        responseDto.setProductId(12L);
        responseDto.setRecommendationType(RecommendationType.FOR_YOU);
        responseDto.setScore(0.9);
        responseDto.setReasonText("Personalized");
        responseDto.setGeneratedAt(LocalDateTime.now());
        responseDto.setExpiresAt(LocalDateTime.now().plusDays(2));
        responseDto.setStatus(RecommendationStatus.ACTIVE);
    }

    @Test
    void createRecommendationShouldReturn201() throws Exception {
        when(recommendationService.createRecommendation(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 10,
                                  "patientProfileId": 11,
                                  "productId": 12,
                                  "recommendationType": "FOR_YOU",
                                  "score": 0.9,
                                  "reasonText": "Personalized"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void logInteractionShouldRejectMissingInteractionType() throws Exception {
        mockMvc.perform(post("/api/recommendations/1/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(recommendationService, never()).logInteraction(any(), any());
    }
}
