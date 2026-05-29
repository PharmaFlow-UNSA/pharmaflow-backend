package com.pharmaflow.smartfeatures.controller.recommendation;

import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationEventResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationGenerateRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationInteractionRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationReservationRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationReservationSagaResponseDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.service.recommendation.RecommendationReservationSagaService;
import com.pharmaflow.smartfeatures.service.recommendation.RecommendationService;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations")
public class RecommendationController {

  private final RecommendationService recommendationService;
  private final RecommendationReservationSagaService reservationSagaService;

  public RecommendationController(
      RecommendationService recommendationService,
      RecommendationReservationSagaService reservationSagaService) {
    this.recommendationService = recommendationService;
    this.reservationSagaService = reservationSagaService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<List<RecommendationResponseDto>> getRecommendations(
      @RequestParam(required = false) @NullablePositive Long userId,
      @RequestParam(required = false) @NullablePositive Long patientProfileId) {
    return ResponseEntity.ok(recommendationService.getRecommendations(userId, patientProfileId));
  }

  @GetMapping("/{id:-?\\d+}")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<RecommendationResponseDto> getRecommendation(
      @PathVariable @Positive Long id) {
    return ResponseEntity.ok(recommendationService.getRecommendation(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<RecommendationResponseDto> createRecommendation(
      @Valid @RequestBody RecommendationRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(recommendationService.createRecommendation(requestDto));
  }

  @PostMapping("/generate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<RecommendationResponseDto>> generateRecommendations(
      @Valid @RequestBody RecommendationGenerateRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(recommendationService.generateRecommendations(requestDto));
  }

  @PutMapping("/{id:-?\\d+}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<RecommendationResponseDto> updateRecommendation(
      @PathVariable @Positive Long id, @Valid @RequestBody RecommendationRequestDto requestDto) {
    return ResponseEntity.ok(recommendationService.updateRecommendation(id, requestDto));
  }

  @PostMapping("/{id:-?\\d+}/interactions")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<RecommendationEventResponseDto> logInteraction(
      @PathVariable @Positive Long id,
      @Valid @RequestBody RecommendationInteractionRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(recommendationService.logInteraction(id, requestDto));
  }

  @PostMapping("/{id:-?\\d+}/reserve")
  @PreAuthorize("hasAnyRole('USER', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<RecommendationReservationSagaResponseDto> reserveRecommendation(
      @PathVariable @Positive Long id,
      @Valid @RequestBody RecommendationReservationRequestDto requestDto) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(reservationSagaService.reserveRecommendation(id, requestDto));
  }

  @GetMapping("/{id:-?\\d+}/interactions")
  @PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'PHARMACIST', 'ADMIN')")
  public ResponseEntity<List<RecommendationEventResponseDto>> getInteractions(
      @PathVariable @Positive Long id) {
    return ResponseEntity.ok(recommendationService.getInteractions(id));
  }
}
