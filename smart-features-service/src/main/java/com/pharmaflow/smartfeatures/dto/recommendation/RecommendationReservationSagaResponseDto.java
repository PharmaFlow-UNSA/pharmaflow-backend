package com.pharmaflow.smartfeatures.dto.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationReservationSagaStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationReservationSagaResponseDto {

  private Long id;
  private String correlationId;
  private Long recommendationId;
  private Long userId;
  private Long patientProfileId;
  private Long productId;
  private Long pharmacyId;
  private Integer quantity;
  private Long reservationId;
  private RecommendationReservationSagaStatus status;
  private String failureReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
