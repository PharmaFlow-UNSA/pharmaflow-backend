package com.pharmaflow.smartfeatures.service.recommendation;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationReservationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationReservationSagaResponseDto;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationReservationSagaStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import com.pharmaflow.smartfeatures.messaging.saga.RecommendationReservationFinalizedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationCompensationRequestedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationCompensatedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationCreatedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationRejectedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.ReservationRequestedEvent;
import com.pharmaflow.smartfeatures.messaging.saga.SmartSagaEventPublisher;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import com.pharmaflow.smartfeatures.model.recommendation.RecommendationEvent;
import com.pharmaflow.smartfeatures.model.recommendation.RecommendationReservationSaga;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationEventRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationReservationSagaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RecommendationReservationSagaService {

  private final RecommendationRepository recommendationRepository;
  private final RecommendationEventRepository recommendationEventRepository;
  private final RecommendationReservationSagaRepository sagaRepository;
  private final SmartSagaEventPublisher eventPublisher;

  public RecommendationReservationSagaService(
      RecommendationRepository recommendationRepository,
      RecommendationEventRepository recommendationEventRepository,
      RecommendationReservationSagaRepository sagaRepository,
      SmartSagaEventPublisher eventPublisher) {
    this.recommendationRepository = recommendationRepository;
    this.recommendationEventRepository = recommendationEventRepository;
    this.sagaRepository = sagaRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public RecommendationReservationSagaResponseDto reserveRecommendation(
      Long recommendationId, RecommendationReservationRequestDto requestDto) {
    Recommendation recommendation = findRecommendationById(recommendationId);
    refreshStatusIfExpired(recommendation);
    if (recommendation.getStatus() != RecommendationStatus.ACTIVE) {
      throw new BadRequestException("Only active recommendations can be reserved.");
    }

    LocalDateTime now = LocalDateTime.now();
    RecommendationReservationSaga saga =
        RecommendationReservationSaga.builder()
            .correlationId(UUID.randomUUID().toString())
            .recommendation(recommendation)
            .userId(recommendation.getUserId())
            .patientProfileId(recommendation.getPatientProfileId())
            .productId(recommendation.getProductId())
            .pharmacyId(requestDto.getPharmacyId())
            .quantity(requestDto.getQuantity())
            .status(RecommendationReservationSagaStatus.PENDING)
            .createdAt(now)
            .updatedAt(now)
            .build();
    RecommendationReservationSaga savedSaga = sagaRepository.save(saga);
    saveRecommendationEvent(recommendation, RecommendationEventType.RESERVATION_REQUESTED, now);

    ReservationRequestedEvent event =
        new ReservationRequestedEvent(
            savedSaga.getCorrelationId(),
            recommendation.getRecommendationId(),
            savedSaga.getUserId(),
            savedSaga.getPatientProfileId(),
            savedSaga.getProductId(),
            savedSaga.getPharmacyId(),
            savedSaga.getQuantity(),
            now,
            requestDto.getExpiresAt());
    afterCommit(() -> eventPublisher.publishReservationRequested(event));

    return toResponseDto(savedSaga);
  }

  @Transactional
  public void handleReservationCreated(ReservationCreatedEvent event) {
    RecommendationReservationSaga saga = findSagaByCorrelationId(event.correlationId());
    if (saga.getStatus() == RecommendationReservationSagaStatus.COMPLETED) {
      return;
    }
    if (saga.getStatus() != RecommendationReservationSagaStatus.PENDING) {
      requestCompensationAfterCommit(saga, "Reservation created for non-pending saga.");
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    saga.setReservationId(event.reservationId());
    saga.setStatus(RecommendationReservationSagaStatus.COMPLETED);
    saga.setFailureReason(null);
    saga.setUpdatedAt(now);
    sagaRepository.save(saga);
    saveRecommendationEvent(
        saga.getRecommendation(), RecommendationEventType.RESERVATION_CONFIRMED, now);

    afterCommit(
        () ->
            eventPublisher.publishReservationFinalized(
                new RecommendationReservationFinalizedEvent(
                    saga.getCorrelationId(),
                    saga.getRecommendation().getRecommendationId(),
                    saga.getReservationId(),
                    now)));
  }

  @Transactional
  public void handleReservationRejected(ReservationRejectedEvent event) {
    RecommendationReservationSaga saga = findSagaByCorrelationId(event.correlationId());
    if (saga.getStatus() == RecommendationReservationSagaStatus.FAILED
        || saga.getStatus() == RecommendationReservationSagaStatus.COMPLETED) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    saga.setStatus(RecommendationReservationSagaStatus.FAILED);
    saga.setFailureReason(event.reason());
    saga.setUpdatedAt(now);
    sagaRepository.save(saga);
    saveRecommendationEvent(saga.getRecommendation(), RecommendationEventType.RESERVATION_FAILED, now);
  }

  @Transactional
  public void handleReservationCompensated(ReservationCompensatedEvent event) {
    RecommendationReservationSaga saga = findSagaByCorrelationId(event.correlationId());
    if (saga.getStatus() == RecommendationReservationSagaStatus.COMPENSATED) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    saga.setReservationId(event.reservationId());
    saga.setStatus(RecommendationReservationSagaStatus.COMPENSATED);
    saga.setUpdatedAt(now);
    sagaRepository.save(saga);
    saveRecommendationEvent(
        saga.getRecommendation(), RecommendationEventType.RESERVATION_COMPENSATED, now);
  }

  private void requestCompensationAfterCommit(
      RecommendationReservationSaga saga, String failureReason) {
    LocalDateTime now = LocalDateTime.now();
    saga.setStatus(RecommendationReservationSagaStatus.COMPENSATION_REQUESTED);
    saga.setFailureReason(failureReason);
    saga.setUpdatedAt(now);
    sagaRepository.save(saga);
    saveRecommendationEvent(saga.getRecommendation(), RecommendationEventType.RESERVATION_FAILED, now);

    afterCommit(
        () ->
            eventPublisher.publishCompensationRequested(
                new ReservationCompensationRequestedEvent(
                    saga.getCorrelationId(), saga.getReservationId(), failureReason, now)));
  }

  private Recommendation findRecommendationById(Long id) {
    return recommendationRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Recommendation not found with id: " + id));
  }

  private RecommendationReservationSaga findSagaByCorrelationId(String correlationId) {
    return sagaRepository
        .findByCorrelationId(correlationId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Recommendation reservation saga not found with correlationId: "
                        + correlationId));
  }

  private void saveRecommendationEvent(
      Recommendation recommendation, RecommendationEventType eventType, LocalDateTime eventTime) {
    recommendationEventRepository.save(
        RecommendationEvent.builder()
            .recommendation(recommendation)
            .eventType(eventType)
            .eventTime(eventTime)
            .build());
  }

  private void refreshStatusIfExpired(Recommendation recommendation) {
    if (recommendation.getStatus() == RecommendationStatus.ACTIVE
        && recommendation.getExpiresAt() != null
        && recommendation.getExpiresAt().isBefore(LocalDateTime.now())) {
      recommendation.setStatus(RecommendationStatus.EXPIRED);
      recommendationRepository.save(recommendation);
    }
  }

  private void afterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }

  private RecommendationReservationSagaResponseDto toResponseDto(
      RecommendationReservationSaga saga) {
    return RecommendationReservationSagaResponseDto.builder()
        .id(saga.getSagaId())
        .correlationId(saga.getCorrelationId())
        .recommendationId(saga.getRecommendation().getRecommendationId())
        .userId(saga.getUserId())
        .patientProfileId(saga.getPatientProfileId())
        .productId(saga.getProductId())
        .pharmacyId(saga.getPharmacyId())
        .quantity(saga.getQuantity())
        .reservationId(saga.getReservationId())
        .status(saga.getStatus())
        .failureReason(saga.getFailureReason())
        .createdAt(saga.getCreatedAt())
        .updatedAt(saga.getUpdatedAt())
        .build();
  }
}
