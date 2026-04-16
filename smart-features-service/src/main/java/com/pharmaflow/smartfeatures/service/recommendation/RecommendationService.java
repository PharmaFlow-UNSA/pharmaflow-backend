package com.pharmaflow.smartfeatures.service.recommendation;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationEventResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationInteractionRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.mapper.recommendation.RecommendationMapper;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.model.recommendation.RecommendationEvent;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationEventRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationEventRepository recommendationEventRepository;
    private final RecommendationMapper recommendationMapper;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            RecommendationEventRepository recommendationEventRepository,
            RecommendationMapper recommendationMapper) {
        this.recommendationRepository = recommendationRepository;
        this.recommendationEventRepository = recommendationEventRepository;
        this.recommendationMapper = recommendationMapper;
    }

    @Transactional
    public List<RecommendationResponseDto> getRecommendations(Long userId, Long patientProfileId) {
        List<Recommendation> recommendations = userId != null
                ? recommendationRepository.findByUserIdOrderByGeneratedAtDesc(userId)
                : patientProfileId != null
                        ? recommendationRepository.findByPatientProfileIdOrderByGeneratedAtDesc(patientProfileId)
                        : recommendationRepository.findAllByOrderByGeneratedAtDesc();

        if (userId != null && patientProfileId != null) {
            recommendations = recommendations.stream()
                    .filter(recommendation -> patientProfileId.equals(recommendation.getPatientProfileId()))
                    .toList();
        }

        recommendations.forEach(this::refreshStatusIfExpired);
        return recommendations.stream().map(recommendationMapper::toResponseDto).toList();
    }

    @Transactional
    public RecommendationResponseDto getRecommendation(Long id) {
        Recommendation recommendation = findRecommendationById(id);
        refreshStatusIfExpired(recommendation);
        return recommendationMapper.toResponseDto(recommendation);
    }

    @Transactional
    public RecommendationResponseDto createRecommendation(RecommendationRequestDto requestDto) {
        ensureActiveRecommendationIsUnique(requestDto, null);

        Recommendation recommendation = recommendationMapper.toEntity(requestDto);
        recommendation.setReasonText(TextSanitizer.sanitizeOptionalText(requestDto.getReasonText()));
        recommendation.setGeneratedAt(LocalDateTime.now());
        validateExpiresAt(recommendation.getGeneratedAt(), recommendation.getExpiresAt());
        recommendation.setStatus(RecommendationStatus.ACTIVE);

        return recommendationMapper.toResponseDto(recommendationRepository.save(recommendation));
    }

    @Transactional
    public RecommendationResponseDto updateRecommendation(Long id, RecommendationRequestDto requestDto) {
        Recommendation recommendation = findRecommendationById(id);
        refreshStatusIfExpired(recommendation);
        if (recommendation.getStatus() != RecommendationStatus.ACTIVE) {
            throw new BadRequestException("Only active recommendations can be updated.");
        }

        ensureActiveRecommendationIsUnique(requestDto, id);
        recommendationMapper.updateEntity(requestDto, recommendation);
        recommendation.setReasonText(TextSanitizer.sanitizeOptionalText(requestDto.getReasonText()));
        validateExpiresAt(recommendation.getGeneratedAt(), recommendation.getExpiresAt());

        return recommendationMapper.toResponseDto(recommendationRepository.save(recommendation));
    }

    @Transactional
    public RecommendationEventResponseDto logInteraction(Long recommendationId, RecommendationInteractionRequestDto requestDto) {
        Recommendation recommendation = findRecommendationById(recommendationId);
        refreshStatusIfExpired(recommendation);

        RecommendationEvent event = RecommendationEvent.builder()
                .recommendation(recommendation)
                .eventType(requestDto.getInteractionType())
                .eventTime(LocalDateTime.now())
                .build();

        RecommendationEvent savedEvent = recommendationEventRepository.save(event);
        if (requestDto.getInteractionType() == RecommendationEventType.DISMISSED) {
            recommendation.setStatus(RecommendationStatus.DISMISSED);
            recommendationRepository.save(recommendation);
        }

        return recommendationMapper.toEventResponseDto(savedEvent);
    }

    @Transactional(readOnly = true)
    public List<RecommendationEventResponseDto> getInteractions(Long recommendationId) {
        findRecommendationById(recommendationId);
        return recommendationEventRepository.findByRecommendationRecommendationIdOrderByEventTimeDesc(recommendationId)
                .stream()
                .map(recommendationMapper::toEventResponseDto)
                .toList();
    }

    private Recommendation findRecommendationById(Long id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + id));
    }

    private void validateExpiresAt(LocalDateTime generatedAt, LocalDateTime expiresAt) {
        if (expiresAt != null && expiresAt.isBefore(generatedAt)) {
            throw new BadRequestException("expiresAt must not be before generatedAt.");
        }
    }

    private void refreshStatusIfExpired(Recommendation recommendation) {
        if (recommendation.getStatus() == RecommendationStatus.ACTIVE
                && recommendation.getExpiresAt() != null
                && recommendation.getExpiresAt().isBefore(LocalDateTime.now())) {
            recommendation.setStatus(RecommendationStatus.EXPIRED);
            recommendationRepository.save(recommendation);
        }
    }

    private void ensureActiveRecommendationIsUnique(RecommendationRequestDto requestDto, Long currentId) {
        boolean duplicateExists;
        if (requestDto.getPatientProfileId() == null) {
            duplicateExists = currentId == null
                    ? recommendationRepository.existsByUserIdAndProductIdAndRecommendationTypeAndStatus(
                            requestDto.getUserId(),
                            requestDto.getProductId(),
                            requestDto.getRecommendationType(),
                            RecommendationStatus.ACTIVE)
                    : recommendationRepository.existsByUserIdAndProductIdAndRecommendationTypeAndStatusAndRecommendationIdNot(
                            requestDto.getUserId(),
                            requestDto.getProductId(),
                            requestDto.getRecommendationType(),
                            RecommendationStatus.ACTIVE,
                            currentId);
        } else {
            duplicateExists = currentId == null
                    ? recommendationRepository.existsByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatus(
                            requestDto.getUserId(),
                            requestDto.getPatientProfileId(),
                            requestDto.getProductId(),
                            requestDto.getRecommendationType(),
                            RecommendationStatus.ACTIVE)
                    : recommendationRepository
                            .existsByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatusAndRecommendationIdNot(
                                    requestDto.getUserId(),
                                    requestDto.getPatientProfileId(),
                                    requestDto.getProductId(),
                                    requestDto.getRecommendationType(),
                                    RecommendationStatus.ACTIVE,
                                    currentId);
        }

        if (duplicateExists) {
            throw new DuplicateResourceException("An active recommendation with the same dimensions already exists.");
        }
    }
}
