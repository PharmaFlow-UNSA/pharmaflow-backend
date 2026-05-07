package com.pharmaflow.smartfeatures.dto.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationInteractionRequestDto {

  @NotNull(message = "interactionType is required")
  private RecommendationEventType interactionType;
}
