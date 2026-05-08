package com.pharmaflow.smartfeatures.repositories.symptom;

import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SymptomRepository extends JpaRepository<Symptom, Long> {

  List<Symptom> findAllByIsActiveTrueOrderByNameAsc();

  @Query(
      """
            select s
            from Symptom s
            where s.isActive = true
              and (
                    lower(s.name) like lower(concat('%', :query, '%'))
                 or lower(coalesce(s.description, '')) like lower(concat('%', :query, '%'))
              )
            order by s.name asc
            """)
  List<Symptom> searchActiveSymptoms(@Param("query") String query);

  boolean existsByNormalizedName(String normalizedName);

  boolean existsByNormalizedNameAndSymptomIdNot(String normalizedName, Long symptomId);
}
