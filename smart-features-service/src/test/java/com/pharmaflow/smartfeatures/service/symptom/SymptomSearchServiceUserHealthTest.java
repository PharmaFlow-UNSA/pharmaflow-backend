package com.pharmaflow.smartfeatures.service.symptom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.SubstanceSnapshot;
import com.pharmaflow.smartfeatures.dto.symptom.SymptomProductMatchResponseDto;
import com.pharmaflow.smartfeatures.dto.userhealth.AllergySnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearch;
import com.pharmaflow.smartfeatures.model.symptom.SymptomSearchItem;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomProductMatchRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchItemRepository;
import com.pharmaflow.smartfeatures.repositories.symptom.SymptomSearchRepository;
import com.pharmaflow.smartfeatures.testsupport.SmartFeaturesIntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class SymptomSearchServiceUserHealthTest extends SmartFeaturesIntegrationTestSupport {

  @Autowired private SymptomSearchService symptomSearchService;

  @Autowired private SymptomRepository symptomRepository;

  @Autowired private SymptomSearchRepository symptomSearchRepository;

  @Autowired private SymptomSearchItemRepository symptomSearchItemRepository;

  @Autowired private SymptomProductMatchRepository symptomProductMatchRepository;

  @MockBean private ProductCatalogClient productCatalogClient;

  @MockBean private UserHealthClient userHealthClient;

  @Test
  void getMatchesShouldUseRealServiceAndDatabaseWhileExcludingAllergyConflicts() {
    Symptom symptom =
        symptomRepository.save(
            Symptom.builder()
                .name("Headache")
                .description("Head pain")
                .severityLevel(SymptomSeverityLevel.MEDIUM)
                .isActive(true)
                .build());
    SymptomSearch search =
        symptomSearchRepository.save(
            SymptomSearch.builder()
                .userId(10L)
                .searchQuery("headache")
                .searchedAt(LocalDateTime.now())
                .build());
    symptomSearchItemRepository.save(
        SymptomSearchItem.builder().search(search).symptom(symptom).build());

    ProductSnapshot conflicting = product(3L, "Ibuprofen headache relief", "Ibuprofen");
    ProductSnapshot allowed = product(4L, "Paracetamol headache tablets", "Paracetamol");
    symptomProductMatchRepository.save(
        SymptomProductMatch.builder()
            .symptom(symptom)
            .productId(allowed.getId())
            .relevanceScore(0.9)
            .matchReason("Curated headache product")
            .build());

    when(userHealthClient.getUser(10L)).thenReturn(Optional.of(userWithAllergy("Ibuprofen")));
    when(productCatalogClient.getActiveProducts()).thenReturn(List.of(conflicting, allowed));

    List<SymptomProductMatchResponseDto> response =
        symptomSearchService.getMatches(search.getSearchId());

    assertThat(response)
        .extracting(SymptomProductMatchResponseDto::getProductId)
        .containsExactly(allowed.getId());
    assertThat(response.get(0).getMatchedSymptomIds()).containsExactly(symptom.getSymptomId());
  }

  private ProductSnapshot product(Long id, String name, String substanceName) {
    ProductSnapshot product = new ProductSnapshot();
    product.setId(id);
    product.setName(name);
    product.setIsActive(true);
    SubstanceSnapshot substance = new SubstanceSnapshot();
    substance.setCommonName(substanceName);
    product.setSubstances(List.of(substance));
    return product;
  }

  private UserHealthSnapshot userWithAllergy(String activeSubstance) {
    AllergySnapshot allergy = new AllergySnapshot();
    allergy.setAllergen(activeSubstance);
    allergy.setActiveSubstance(activeSubstance);
    PatientHealthProfileSnapshot profile = new PatientHealthProfileSnapshot();
    profile.setId(20L);
    profile.setAllergies(List.of(allergy));
    UserHealthSnapshot user = new UserHealthSnapshot();
    user.setId(10L);
    user.setPatientProfile(profile);
    return user;
  }
}
