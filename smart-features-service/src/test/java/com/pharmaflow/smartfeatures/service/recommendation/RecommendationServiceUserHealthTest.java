package com.pharmaflow.smartfeatures.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.client.user.UserHealthClient;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.SubstanceSnapshot;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationGenerateRequestDto;
import com.pharmaflow.smartfeatures.dto.recommendation.RecommendationResponseDto;
import com.pharmaflow.smartfeatures.dto.userhealth.AllergySnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.repositories.recommendation.RecommendationRepository;
import com.pharmaflow.smartfeatures.testsupport.SmartFeaturesIntegrationTestSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class RecommendationServiceUserHealthTest extends SmartFeaturesIntegrationTestSupport {

  @Autowired private RecommendationService recommendationService;

  @Autowired private RecommendationRepository recommendationRepository;

  @MockBean private ProductCatalogClient productCatalogClient;

  @MockBean private UserHealthClient userHealthClient;

  @Test
  void generateRecommendationsShouldUseRealServiceAndDatabaseWhileExcludingAllergyConflicts() {
    ProductSnapshot seed = product(1L, "Cold relief", "Paracetamol");
    ProductSnapshot conflicting = product(2L, "Ibuprofen gel", "Ibuprofen");
    ProductSnapshot allowed = product(3L, "Paracetamol plus", "Paracetamol");
    RecommendationGenerateRequestDto request =
        new RecommendationGenerateRequestDto(
            10L, null, RecommendationType.SIMILAR_PRODUCT, seed.getId(), null, 5, null);

    when(userHealthClient.getUser(10L)).thenReturn(Optional.of(userWithAllergy("Ibuprofen")));
    when(productCatalogClient.getProduct(seed.getId())).thenReturn(Optional.of(seed));
    when(productCatalogClient.getActiveProducts()).thenReturn(List.of(conflicting, allowed));

    List<RecommendationResponseDto> recommendations =
        recommendationService.generateRecommendations(request);

    assertThat(recommendations)
        .extracting(RecommendationResponseDto::getProductId)
        .containsExactly(allowed.getId());
    assertThat(recommendationRepository.findByUserIdOrderByGeneratedAtDesc(10L))
        .extracting(recommendation -> recommendation.getProductId())
        .containsExactly(allowed.getId());
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
