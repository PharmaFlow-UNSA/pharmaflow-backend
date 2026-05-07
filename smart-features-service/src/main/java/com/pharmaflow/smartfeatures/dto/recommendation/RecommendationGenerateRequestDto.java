package com.pharmaflow.smartfeatures.dto.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationGenerateRequestDto {

  @NotNull(message = "userId is required")
  @Positive(message = "userId must be positive")
  private Long userId;

  @NullablePositive(message = "patientProfileId must be positive")
  private Long patientProfileId;

  @NotNull(message = "recommendationType is required")
  private RecommendationType recommendationType;

  @NullablePositive(message = "seedProductId must be positive")
  private Long seedProductId;

  private List<@Positive(message = "symptomId must be positive") Long> symptomIds;

  @Min(value = 1, message = "limit must be at least 1")
  @Max(value = 50, message = "limit must be at most 50")
  private Integer limit;

  private LocalDateTime expiresAt;
}
