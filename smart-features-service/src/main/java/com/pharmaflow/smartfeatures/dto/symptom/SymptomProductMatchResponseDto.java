package com.pharmaflow.smartfeatures.dto.symptom;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SymptomProductMatchResponseDto {

  private Long id;
  private Long symptomId;
  private Long productId;
  private Double relevanceScore;
  private String matchReason;
  private List<Long> matchedSymptomIds;
}
