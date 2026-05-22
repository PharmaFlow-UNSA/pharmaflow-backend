package com.pharmaflow.smartfeatures.dto.recommendation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationReservationRequestDto {

  @NotNull(message = "pharmacyId is required")
  @Positive(message = "pharmacyId must be positive")
  private Long pharmacyId;

  @NotNull(message = "quantity is required")
  @Positive(message = "quantity must be positive")
  private Integer quantity;

  @Future(message = "expiresAt must be in the future")
  private LocalDateTime expiresAt;
}
