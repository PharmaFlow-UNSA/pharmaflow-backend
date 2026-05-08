package com.pharmaflow.smartfeatures.service.productintelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmaflow.smartfeatures.dto.product.CategorySnapshot;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.SubstanceSnapshot;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductFeatureExtractorTest {

  private final ProductFeatureExtractor extractor = new ProductFeatureExtractor();

  @Test
  void extractShouldBuildNormalizedProductTokensAndEmbeddingText() {
    ProductSnapshot product =
        product(
            101L,
            "Cough Relief Syrup",
            "Respira",
            category(10L, "Respiratory Care", "Cough and cold products"),
            List.of(substance("Dextromethorphan", "Cough suppressant")),
            "OTC",
            false);
    product.setDescription("Dry cough relief for irritated throat");
    product.setPackageSize("150 ml bottle");

    ProductFeatures features = extractor.extract(product);

    assertThat(features.tokens())
        .contains(
            "cough", "relief", "syrup", "respira", "respiratory", "dextromethorphan", "150", "ml");
    assertThat(features.categoryTokens()).contains("respiratory", "care");
    assertThat(features.substanceTokens()).contains("dextromethorphan", "suppressant");
    assertThat(features.productType()).isEqualTo("otc");
    assertThat(features.requiresPrescription()).isFalse();
    assertThat(features.embeddingText())
        .contains("Cough Relief Syrup", "Respiratory Care", "dextromethorphan");
  }

  @Test
  void symptomTokensShouldNormalizeNamesDescriptionsAndTags() {
    Symptom symptom =
        Symptom.builder()
            .symptomId(5L)
            .name("Dry Cough")
            .description("Irritated THROAT")
            .severityLevel(SymptomSeverityLevel.MEDIUM)
            .isActive(true)
            .tags(List.of(" Cough ", "Night-Time"))
            .build();

    assertThat(extractor.symptomTokens(List.of(symptom)))
        .contains("dry", "cough", "irritated", "throat", "night", "time");
  }

  private static ProductSnapshot product(
      Long id,
      String name,
      String brandName,
      CategorySnapshot category,
      List<SubstanceSnapshot> substances,
      String productType,
      boolean requiresPrescription) {
    ProductSnapshot product = new ProductSnapshot();
    product.setId(id);
    product.setName(name);
    product.setBrandName(brandName);
    product.setCategory(category);
    product.setSubstances(substances);
    product.setProductType(productType);
    product.setRequiresPrescription(requiresPrescription);
    product.setIsActive(true);
    return product;
  }

  private static CategorySnapshot category(Long id, String name, String description) {
    CategorySnapshot category = new CategorySnapshot();
    category.setId(id);
    category.setName(name);
    category.setDescription(description);
    return category;
  }

  private static SubstanceSnapshot substance(String inn, String description) {
    SubstanceSnapshot substance = new SubstanceSnapshot();
    substance.setInn(inn);
    substance.setDescription(description);
    return substance;
  }
}
