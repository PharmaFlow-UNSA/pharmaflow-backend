package com.pharmaflow.smartfeatures.repositories.recommendation;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.model.recommendation.Recommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findAllByOrderByGeneratedAtDesc();

    List<Recommendation> findByUserIdOrderByGeneratedAtDesc(Long userId);

    List<Recommendation> findByPatientProfileIdOrderByGeneratedAtDesc(Long patientProfileId);

    boolean existsByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatus(
            Long userId,
            Long patientProfileId,
            Long productId,
            RecommendationType recommendationType,
            RecommendationStatus status);

    boolean existsByUserIdAndProductIdAndRecommendationTypeAndStatus(
            Long userId, Long productId, RecommendationType recommendationType, RecommendationStatus status);

    boolean existsByUserIdAndProductIdAndRecommendationTypeAndStatusAndRecommendationIdNot(
            Long userId,
            Long productId,
            RecommendationType recommendationType,
            RecommendationStatus status,
            Long recommendationId);

    boolean existsByUserIdAndPatientProfileIdAndProductIdAndRecommendationTypeAndStatusAndRecommendationIdNot(
            Long userId,
            Long patientProfileId,
            Long productId,
            RecommendationType recommendationType,
            RecommendationStatus status,
            Long recommendationId);
}
