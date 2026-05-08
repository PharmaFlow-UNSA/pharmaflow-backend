package com.pharmaflow.smartfeatures.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationInteractionRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationRequestDto;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationEventRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import com.pharmaflow.smartfeatures.testsupport.SmartFeaturesIntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecommendationServiceTest extends SmartFeaturesIntegrationTestSupport {

  @Autowired private RecommendationService recommendationService;

  @Autowired private RecommendationRepository recommendationRepository;

  @Autowired private RecommendationEventRepository recommendationEventRepository;

  @Test
  void getRecommendationShouldExpireActiveRecommendationWhenPastDue() {
    Recommendation recommendation =
        recommendationRepository.save(
            Recommendation.builder()
                .userId(10L)
                .productId(20L)
                .recommendationType(RecommendationType.FOR_YOU)
                .generatedAt(LocalDateTime.now().minusDays(3))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .status(RecommendationStatus.ACTIVE)
                .build());

    assertThat(
            recommendationService
                .getRecommendation(recommendation.getRecommendationId())
                .getStatus())
        .isEqualTo(RecommendationStatus.EXPIRED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM recommendation WHERE recommendation_id = ?",
                String.class,
                recommendation.getRecommendationId()))
        .isEqualTo("EXPIRED");
  }

  @Test
  void logInteractionShouldDismissRecommendationAndPersistEvent() {
    Recommendation recommendation =
        recommendationRepository.save(
            Recommendation.builder()
                .userId(10L)
                .productId(20L)
                .recommendationType(RecommendationType.FOR_YOU)
                .generatedAt(LocalDateTime.now().minusHours(3))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .status(RecommendationStatus.ACTIVE)
                .build());

    recommendationService.logInteraction(
        recommendation.getRecommendationId(),
        new RecommendationInteractionRequestDto(RecommendationEventType.DISMISSED));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM recommendation WHERE recommendation_id = ?",
                String.class,
                recommendation.getRecommendationId()))
        .isEqualTo("DISMISSED");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_event WHERE recommendation_id = ?",
                Integer.class,
                recommendation.getRecommendationId()))
        .isEqualTo(1);
  }

  @Test
  void logInteractionShouldRejectExpiredRecommendationAfterRefreshingStatus() {
    Recommendation recommendation =
        recommendationRepository.save(
            Recommendation.builder()
                .userId(10L)
                .productId(20L)
                .recommendationType(RecommendationType.FOR_YOU)
                .generatedAt(LocalDateTime.now().minusDays(3))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .status(RecommendationStatus.ACTIVE)
                .build());

    assertThatThrownBy(
            () ->
                recommendationService.logInteraction(
                    recommendation.getRecommendationId(),
                    new RecommendationInteractionRequestDto(RecommendationEventType.DISMISSED)))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Only active recommendations can receive interactions.");

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM recommendation WHERE recommendation_id = ?",
                String.class,
                recommendation.getRecommendationId()))
        .isEqualTo("EXPIRED");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_event WHERE recommendation_id = ?",
                Integer.class,
                recommendation.getRecommendationId()))
        .isEqualTo(0);
  }

  @Test
  void createRecommendationShouldRejectDuplicateActiveRecommendation() {
    RecommendationRequestDto requestDto =
        new RecommendationRequestDto(
            10L,
            11L,
            12L,
            RecommendationType.FOR_YOU,
            0.9,
            "Useful",
            LocalDateTime.now().plusDays(1));
    recommendationService.createRecommendation(requestDto);

    assertThatThrownBy(() -> recommendationService.createRecommendation(requestDto))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessage("An active recommendation with the same dimensions already exists.");
  }
}
