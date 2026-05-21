package com.pharmaflow.smartfeatures.repositories.recommendation;

import com.pharmaflow.smartfeatures.model.recommendation.RecommendationReservationSaga;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationReservationSagaRepository
    extends JpaRepository<RecommendationReservationSaga, Long> {

  Optional<RecommendationReservationSaga> findByCorrelationId(String correlationId);
}
