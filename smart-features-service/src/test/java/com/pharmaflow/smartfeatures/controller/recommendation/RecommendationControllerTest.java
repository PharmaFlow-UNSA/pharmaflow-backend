package com.pharmaflow.smartfeatures.controller.recommendation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationEventResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationReservationSagaResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationReservationSagaStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.exception.GlobalExceptionHandler;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.service.recommendation.RecommendationReservationSagaService;
import com.pharmaflow.smartfeatures.service.recommendation.RecommendationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private RecommendationService recommendationService;

  @MockBean private RecommendationReservationSagaService reservationSagaService;

  private RecommendationResponseDto responseDto;
  private RecommendationEventResponseDto eventResponseDto;

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

    eventResponseDto = new RecommendationEventResponseDto();
    eventResponseDto.setId(2L);
    eventResponseDto.setRecommendationId(1L);
    eventResponseDto.setEventType(RecommendationEventType.VIEWED);
    eventResponseDto.setEventTime(LocalDateTime.now());
  }

  @Test
  void getRecommendationsShouldReturn200AndList() throws Exception {
    when(recommendationService.getRecommendations(10L, 11L)).thenReturn(List.of(responseDto));

    mockMvc
        .perform(get("/api/recommendations").param("userId", "10").param("patientProfileId", "11"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].recommendationType").value("FOR_YOU"));
  }

  @Test
  void getRecommendationsShouldRejectInvalidFilter() throws Exception {
    mockMvc
        .perform(get("/api/recommendations").param("userId", "-10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSTRAINT_VIOLATION"));

    verify(recommendationService, never()).getRecommendations(any(), any());
  }

  @Test
  void getRecommendationShouldReturn200WhenFound() throws Exception {
    when(recommendationService.getRecommendation(1L)).thenReturn(responseDto);

    mockMvc
        .perform(get("/api/recommendations/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void getRecommendationShouldReturn404WhenMissing() throws Exception {
    when(recommendationService.getRecommendation(404L))
        .thenThrow(new ResourceNotFoundException("Recommendation not found with id: 404"));

    mockMvc
        .perform(get("/api/recommendations/404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void createRecommendationShouldReturn201() throws Exception {
    when(recommendationService.createRecommendation(any())).thenReturn(responseDto);

    mockMvc
        .perform(
            post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
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
  void generateRecommendationsShouldReturn201AndList() throws Exception {
    when(recommendationService.generateRecommendations(any())).thenReturn(List.of(responseDto));

    mockMvc
        .perform(
            post("/api/recommendations/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userId": 10,
                                  "patientProfileId": 11,
                                  "recommendationType": "FOR_YOU",
                                  "symptomIds": [1],
                                  "limit": 5
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].productId").value(12));
  }

  @Test
  void generateRecommendationsShouldRejectInvalidLimit() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userId": 10,
                                  "recommendationType": "FOR_YOU",
                                  "limit": 0
                                }
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(recommendationService, never()).generateRecommendations(any());
  }

  @Test
  void updateRecommendationShouldReturn200WhenFound() throws Exception {
    when(recommendationService.updateRecommendation(eq(1L), any())).thenReturn(responseDto);

    mockMvc
        .perform(
            put("/api/recommendations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userId": 10,
                                  "patientProfileId": 11,
                                  "productId": 12,
                                  "recommendationType": "FOR_YOU",
                                  "score": 0.9,
                                  "reasonText": "Personalized"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void updateRecommendationShouldReturn404WhenMissing() throws Exception {
    when(recommendationService.updateRecommendation(eq(404L), any()))
        .thenThrow(new ResourceNotFoundException("Recommendation not found with id: 404"));

    mockMvc
        .perform(
            put("/api/recommendations/404")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userId": 10,
                                  "patientProfileId": 11,
                                  "productId": 12,
                                  "recommendationType": "FOR_YOU",
                                  "score": 0.9,
                                  "reasonText": "Personalized"
                                }
                                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void logInteractionShouldReturn201() throws Exception {
    when(recommendationService.logInteraction(eq(1L), any())).thenReturn(eventResponseDto);

    mockMvc
        .perform(
            post("/api/recommendations/1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"interactionType": "VIEWED"}
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.eventType").value("VIEWED"));
  }

  @Test
  void logInteractionShouldRejectMissingInteractionType() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/1/interactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(recommendationService, never()).logInteraction(any(), any());
  }

  @Test
  void reserveRecommendationShouldReturn202AndSagaStatus() throws Exception {
    RecommendationReservationSagaResponseDto sagaResponse =
        RecommendationReservationSagaResponseDto.builder()
            .id(5L)
            .correlationId("corr-1")
            .recommendationId(1L)
            .userId(10L)
            .productId(12L)
            .pharmacyId(7L)
            .quantity(2)
            .status(RecommendationReservationSagaStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    when(reservationSagaService.reserveRecommendation(eq(1L), any())).thenReturn(sagaResponse);

    mockMvc
        .perform(
            post("/api/recommendations/1/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "pharmacyId": 7,
                      "quantity": 2
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.correlationId").value("corr-1"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void reserveRecommendationShouldRejectInvalidQuantity() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/1/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "pharmacyId": 7,
                      "quantity": 0
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    verify(reservationSagaService, never()).reserveRecommendation(any(), any());
  }

  @Test
  void getInteractionsShouldReturn200AndList() throws Exception {
    when(recommendationService.getInteractions(1L)).thenReturn(List.of(eventResponseDto));

    mockMvc
        .perform(get("/api/recommendations/1/interactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].recommendationId").value(1))
        .andExpect(jsonPath("$[0].eventType").value("VIEWED"));
  }

  @Test
  void getInteractionsShouldReturn404WhenRecommendationMissing() throws Exception {
    when(recommendationService.getInteractions(404L))
        .thenThrow(new ResourceNotFoundException("Recommendation not found with id: 404"));

    mockMvc
        .perform(get("/api/recommendations/404/interactions"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }
}
