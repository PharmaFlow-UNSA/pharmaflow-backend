package com.pharmaflow.smartfeatures.dto.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequestDto {

    @NotNull(message = "userId is required")
    @Positive(message = "userId must be positive")
    private Long userId;

    @NullablePositive(message = "patientProfileId must be positive")
    private Long patientProfileId;

    @NotNull(message = "productId is required")
    @Positive(message = "productId must be positive")
    private Long productId;

    @NotNull(message = "recommendationType is required")
    private RecommendationType recommendationType;

    @DecimalMin(value = "0.0", message = "score must be at least 0.0")
    @DecimalMax(value = "1.0", message = "score must be at most 1.0")
    private Double score;

    @SanitizedTextSize(max = 500, message = "reasonText must not exceed 500 characters")
    private String reasonText;

    private java.time.LocalDateTime expiresAt;
}
