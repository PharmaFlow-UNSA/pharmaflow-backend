package com.pharmaflow.smartfeatures.repositories.recommendation;

import com.pharmaflow.smartfeatures.model.recommendation.RecommendationEvent;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationEventRepository extends JpaRepository<RecommendationEvent, Long> {

  @EntityGraph(attributePaths = "recommendation")
  List<RecommendationEvent> findByRecommendationRecommendationIdOrderByEventTimeDesc(
      Long recommendationId);

  @EntityGraph(attributePaths = "recommendation")
  List<RecommendationEvent> findByRecommendationUserIdOrderByEventTimeDesc(Long userId);
}
