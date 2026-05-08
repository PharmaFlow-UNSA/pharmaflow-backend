package com.pharmaflow.smartfeatures.service.symptom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchItemRequestDto;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomSearchRequestDto;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.exception.DuplicateResourceException;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearch;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomProductMatchRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchItemRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchRepository;
import com.pharmaflow.smartfeatures.testsupport.SmartFeaturesIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SymptomSearchServiceTest extends SmartFeaturesIntegrationTestSupport {

  @Autowired private SymptomSearchService symptomSearchService;

  @Autowired private SymptomRepository symptomRepository;

  @Autowired private SymptomSearchRepository symptomSearchRepository;

  @Autowired private SymptomSearchItemRepository symptomSearchItemRepository;

  @Autowired private SymptomProductMatchRepository symptomProductMatchRepository;

  @Test
  void createSearchShouldTrimQueryAndPersistTimestamp() {
    Long searchId =
        symptomSearchService
            .createSearch(new SymptomSearchRequestDto(10L, 11L, "  dry cough  "))
            .getId();

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT search_query FROM symptom_search WHERE search_id = ?",
                String.class,
                searchId))
        .isEqualTo("dry cough");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT searched_at FROM symptom_search WHERE search_id = ?",
                java.sql.Timestamp.class,
                searchId))
        .isNotNull();
  }

  @Test
  void addItemShouldRejectDuplicateSymptom() {
    SymptomSearch search =
        symptomSearchRepository.save(
            SymptomSearch.builder()
                .userId(10L)
                .searchQuery("fever")
                .searchedAt(java.time.LocalDateTime.now())
                .build());
    Symptom symptom =
        symptomRepository.save(
            Symptom.builder()
                .name("Fever")
                .severityLevel(SymptomSeverityLevel.HIGH)
                .isActive(true)
                .build());

    symptomSearchService.addItem(
        search.getSearchId(), new SymptomSearchItemRequestDto(symptom.getSymptomId()));

    assertThatThrownBy(
            () ->
                symptomSearchService.addItem(
                    search.getSearchId(), new SymptomSearchItemRequestDto(symptom.getSymptomId())))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessage("Symptom is already linked to this search.");
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM symptom_search_item", Integer.class))
        .isEqualTo(1);
  }
}
