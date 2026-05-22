package com.pharmaflow.smartfeatures.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationReservationRequestDto;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationReservationSagaStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationCompensatedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationCreatedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationRejectedEvent;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import com.pharmaflow.smartfeatures.testsupport.SmartFeaturesIntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecommendationReservationSagaServiceTest extends SmartFeaturesIntegrationTestSupport {

  @Autowired private RecommendationReservationSagaService sagaService;

  @Autowired private RecommendationRepository recommendationRepository;

  @Test
  void reserveRecommendationShouldCreatePendingSagaAndRequestEvent() {
    Recommendation recommendation = activeRecommendation();

    var response =
        sagaService.reserveRecommendation(
            recommendation.getRecommendationId(), new RecommendationReservationRequestDto(7L, 2, null));

    assertThat(response.getStatus()).isEqualTo(RecommendationReservationSagaStatus.PENDING);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_reservation_saga WHERE correlation_id = ?",
                Integer.class,
                response.getCorrelationId()))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT event_type FROM recommendation_event WHERE recommendation_id = ?",
                String.class,
                recommendation.getRecommendationId()))
        .isEqualTo("RESERVATION_REQUESTED");
  }

  @Test
  void reserveRecommendationShouldRejectExpiredRecommendation() {
    Recommendation recommendation =
        recommendationRepository.save(
            Recommendation.builder()
                .userId(10L)
                .productId(12L)
                .recommendationType(RecommendationType.FOR_YOU)
                .generatedAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .status(RecommendationStatus.ACTIVE)
                .build());

    assertThatThrownBy(
            () ->
                sagaService.reserveRecommendation(
                    recommendation.getRecommendationId(),
                    new RecommendationReservationRequestDto(7L, 2, null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Only active recommendations can be reserved.");
  }

  @Test
  void handleReservationCreatedShouldCompletePendingSagaIdempotently() {
    String correlationId = createPendingSaga();

    sagaService.handleReservationCreated(
        new ReservationCreatedEvent(correlationId, 99L, "PENDING", LocalDateTime.now()));
    sagaService.handleReservationCreated(
        new ReservationCreatedEvent(correlationId, 99L, "PENDING", LocalDateTime.now()));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM recommendation_reservation_saga WHERE correlation_id = ?",
                String.class,
                correlationId))
        .isEqualTo("COMPLETED");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_event WHERE event_type = 'RESERVATION_CONFIRMED'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void handleReservationRejectedShouldFailPendingSaga() {
    String correlationId = createPendingSaga();

    sagaService.handleReservationRejected(
        new ReservationRejectedEvent(correlationId, "Pharmacy missing", LocalDateTime.now()));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM recommendation_reservation_saga WHERE correlation_id = ?",
                String.class,
                correlationId))
        .isEqualTo("FAILED");
  }

  @Test
  void handleReservationCompensatedShouldMarkSagaCompensated() {
    String correlationId = createPendingSaga();

    sagaService.handleReservationCompensated(
        new ReservationCompensatedEvent(correlationId, 99L, LocalDateTime.now()));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM recommendation_reservation_saga WHERE correlation_id = ?",
                String.class,
                correlationId))
        .isEqualTo("COMPENSATED");
  }

  private String createPendingSaga() {
    Recommendation recommendation = activeRecommendation();
    return sagaService
        .reserveRecommendation(
            recommendation.getRecommendationId(), new RecommendationReservationRequestDto(7L, 2, null))
        .getCorrelationId();
  }

  private Recommendation activeRecommendation() {
    return recommendationRepository.save(
        Recommendation.builder()
            .userId(10L)
            .productId(12L)
            .recommendationType(RecommendationType.FOR_YOU)
            .generatedAt(LocalDateTime.now().minusHours(1))
            .expiresAt(LocalDateTime.now().plusDays(1))
            .status(RecommendationStatus.ACTIVE)
            .build());
  }
}
