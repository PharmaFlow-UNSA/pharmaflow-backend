package com.pharmaflow.smartfeatures.util;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;

public final class RecommendationKeyNormalizer {

  private RecommendationKeyNormalizer() {}

  public static String buildActiveDimensionKey(
      Long userId,
      Long patientProfileId,
      Long productId,
      RecommendationType recommendationType,
      RecommendationStatus status) {
    if (status != RecommendationStatus.ACTIVE) {
      return null;
    }

    return userId
        + "|"
        + normalizeNullable(patientProfileId)
        + "|"
        + productId
        + "|"
        + recommendationType.name();
  }

  private static String normalizeNullable(Long value) {
    return value == null ? "NONE" : value.toString();
  }
}
