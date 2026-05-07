package com.pharmaflow.smartfeatures.dto.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationEventResponseDto {

  private Long id;
  private Long recommendationId;
  private RecommendationEventType eventType;
  private LocalDateTime eventTime;
}
