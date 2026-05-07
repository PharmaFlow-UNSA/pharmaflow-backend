package com.pharmaflow.smartfeatures.repositories.symptom;

import com.pharmaflow.smartfeatures.model.symptom.SymptomSearchItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SymptomSearchItemRepository extends JpaRepository<SymptomSearchItem, Long> {

  @EntityGraph(attributePaths = {"search", "symptom"})
  List<SymptomSearchItem> findBySearchSearchIdOrderBySearchItemIdAsc(Long searchId);

  @EntityGraph(attributePaths = {"search", "symptom"})
  Optional<SymptomSearchItem> findBySearchSearchIdAndSearchItemId(Long searchId, Long searchItemId);

  boolean existsBySearchSearchIdAndSymptomSymptomId(Long searchId, Long symptomId);
}
