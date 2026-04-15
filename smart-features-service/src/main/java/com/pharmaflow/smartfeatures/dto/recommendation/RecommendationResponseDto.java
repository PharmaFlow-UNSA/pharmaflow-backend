package com.pharmaflow.smartfeatures.dto.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {

    private Long id;
    private Long userId;
    private Long patientProfileId;
    private Long productId;
    private RecommendationType recommendationType;
    private Double score;
    private String reasonText;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private RecommendationStatus status;
}
