package com.pharmaflow.smartfeatures.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.config.ModelMapperConfig;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationEventResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationInteractionRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.mapper.recommendation.RecommendationMapper;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.model.recommendation.RecommendationEvent;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationEventRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationEventRepository recommendationEventRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                recommendationRepository,
                recommendationEventRepository,
                new RecommendationMapper(new ModelMapperConfig().modelMapper()));
    }

    @Test
    void getRecommendationShouldExpireActiveRecommendationWhenPastDue() {
        Recommendation recommendation = Recommendation.builder()
                .recommendationId(1L)
                .userId(10L)
                .productId(20L)
                .recommendationType(RecommendationType.FOR_YOU)
                .generatedAt(LocalDateTime.now().minusDays(3))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .status(RecommendationStatus.ACTIVE)
                .build();
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(recommendation)).thenReturn(recommendation);

        RecommendationResponseDto response = recommendationService.getRecommendation(1L);

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
        assertThat(response.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
        verify(recommendationRepository).save(recommendation);
    }

    @Test
    void logInteractionShouldDismissRecommendation() {
        Recommendation recommendation = Recommendation.builder()
                .recommendationId(2L)
                .userId(10L)
                .productId(20L)
                .recommendationType(RecommendationType.FOR_YOU)
                .generatedAt(LocalDateTime.now().minusHours(3))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .status(RecommendationStatus.ACTIVE)
                .build();
        when(recommendationRepository.findById(2L)).thenReturn(Optional.of(recommendation));
        when(recommendationEventRepository.save(any(RecommendationEvent.class))).thenAnswer(invocation -> {
            RecommendationEvent event = invocation.getArgument(0);
            event.setEventId(99L);
            return event;
        });
        when(recommendationRepository.save(recommendation)).thenReturn(recommendation);

        RecommendationEventResponseDto response = recommendationService.logInteraction(
                2L, new RecommendationInteractionRequestDto(RecommendationEventType.DISMISSED));

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.DISMISSED);
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getEventType()).isEqualTo(RecommendationEventType.DISMISSED);
    }

    @Test
    void createRecommendationShouldRejectDuplicateActiveRecommendation() {
        RecommendationRequestDto requestDto = new RecommendationRequestDto(
                10L, 11L, 12L, RecommendationType.FOR_YOU, 0.9, "Useful", LocalDateTime.now().plusDays(1));
        when(recommendationRepository.existsByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatus(
                        10L, 11L, 12L, RecommendationType.FOR_YOU, RecommendationStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> recommendationService.createRecommendation(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("An active recommendation with the same dimensions already exists.");
    }
}
