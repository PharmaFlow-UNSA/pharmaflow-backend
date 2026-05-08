package com.pharmaflow.smartfeatures.repositories.symptom;

import com.pharmaflow.smartfeatures.model.symptom.SymptomSearch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SymptomSearchRepository extends JpaRepository<SymptomSearch, Long> {

  List<SymptomSearch> findAllByOrderBySearchedAtDesc();

  List<SymptomSearch> findByUserIdOrderBySearchedAtDesc(Long userId);

  List<SymptomSearch> findByPatientProfileIdOrderBySearchedAtDesc(Long patientProfileId);
}
