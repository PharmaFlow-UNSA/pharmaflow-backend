package com.pharmaflow.smartfeatures.repositories.symptom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class SymptomRepositoryTest {

  @Autowired private SymptomRepository symptomRepository;

  @BeforeEach
  void setUp() {
    symptomRepository.deleteAll();

    symptomRepository.save(
        buildSymptom("Fever", "Elevated body temperature", SymptomSeverityLevel.HIGH, true));
    symptomRepository.save(
        buildSymptom(
            "Sneezing", "Frequent sneezing and irritation", SymptomSeverityLevel.LOW, true));
    symptomRepository.save(
        buildSymptom(
            "Archived Symptom",
            "Should not appear in active listings",
            SymptomSeverityLevel.MEDIUM,
            false));
  }

  @Test
  void findAllByIsActiveTrueOrderByNameAsc_ShouldReturnOnlyActiveSymptoms() {
    List<Symptom> result = symptomRepository.findAllByIsActiveTrueOrderByNameAsc();

    assertThat(result).extracting(Symptom::getName).containsExactly("Fever", "Sneezing");
  }

  @Test
  void searchActiveSymptoms_ShouldSearchNameAndDescriptionCaseInsensitive() {
    List<Symptom> byName = symptomRepository.searchActiveSymptoms("fev");
    List<Symptom> byDescription = symptomRepository.searchActiveSymptoms("IRRITATION");

    assertThat(byName).extracting(Symptom::getName).containsExactly("Fever");
    assertThat(byDescription).extracting(Symptom::getName).containsExactly("Sneezing");
  }

  @Test
  void existsByNormalizedName_ShouldTreatValuesCaseInsensitive() {
    boolean exists = symptomRepository.existsByNormalizedName("fever");

    assertThat(exists).isTrue();
  }

  @Test
  void saveAndFlush_WhenNormalizedNameDuplicates_ShouldFailWithDataIntegrityViolation() {
    Symptom duplicate =
        buildSymptom("  FEVER ", "Duplicate symptom", SymptomSeverityLevel.LOW, true);

    assertThatThrownBy(() -> symptomRepository.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void
      saveAndFlush_WhenNameDiffersOnlyByCollapsedWhitespace_ShouldFailWithDataIntegrityViolation() {
    symptomRepository.saveAndFlush(
        buildSymptom("Dry Cough", "Cough symptom", SymptomSeverityLevel.MEDIUM, true));

    Symptom duplicate =
        buildSymptom("Dry   Cough", "Whitespace duplicate", SymptomSeverityLevel.MEDIUM, true);

    assertThatThrownBy(() -> symptomRepository.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Symptom buildSymptom(
      String name, String description, SymptomSeverityLevel severityLevel, boolean active) {
    return Symptom.builder()
        .name(name)
        .description(description)
        .severityLevel(severityLevel)
        .isActive(active)
        .build();
  }
}
