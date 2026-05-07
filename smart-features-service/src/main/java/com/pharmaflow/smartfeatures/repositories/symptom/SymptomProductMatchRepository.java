package com.pharmaflow.smartfeatures.repositories.symptom;

import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SymptomProductMatchRepository extends JpaRepository<SymptomProductMatch, Long> {

  @EntityGraph(attributePaths = "symptom")
  List<SymptomProductMatch> findBySymptomSymptomIdOrderByRelevanceScoreDescMatchIdAsc(
      Long symptomId);

  @EntityGraph(attributePaths = "symptom")
  Optional<SymptomProductMatch> findBySymptomSymptomIdAndMatchId(Long symptomId, Long matchId);

  boolean existsBySymptomSymptomIdAndProductId(Long symptomId, Long productId);

  boolean existsBySymptomSymptomIdAndProductIdAndMatchIdNot(
      Long symptomId, Long productId, Long matchId);
}
