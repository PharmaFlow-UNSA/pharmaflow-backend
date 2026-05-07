package com.pharmaflow.smartfeatures.service.productintelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmaflow.smartfeatures.dto.product.CategorySnapshot;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.ProductSubstituteSnapshot;
import com.pharmaflow.smartfeatures.dto.product.SubstanceSnapshot;
import com.pharmaflow.smartfeatures.enums.symptom.SymptomSeverityLevel;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import com.pharmaflow.smartfeatures.service.chatbot.EmbeddingService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProductRankerTest {

  private final ProductRanker ranker =
      new ProductRanker(
          new ProductFeatureExtractor(),
          new EmbeddingSimilarityService(new EmbeddingService("http://localhost:1", false)));

  @Test
  void rankSymptomMatchesShouldCombineCuratedAndMetadataSignalsWithoutEmbeddings() {
    Symptom cough =
        Symptom.builder()
            .symptomId(5L)
            .name("Dry Cough")
            .description("Irritated throat")
            .severityLevel(SymptomSeverityLevel.MEDIUM)
            .isActive(true)
            .tags(List.of("cough", "throat"))
            .build();
    ProductSnapshot coughSyrup =
        product(
            101L,
            "Cough Relief Syrup",
            category(10L, "Respiratory Care"),
            List.of(substance("Dextromethorphan")),
            "OTC",
            false);
    coughSyrup.setDescription("Dry cough suppressant");
    ProductSnapshot vitamin =
        product(
            202L,
            "Vitamin C Tablets",
            category(11L, "Vitamins"),
            List.of(substance("Ascorbic acid")),
            "SUPPLEMENT",
            false);
    SymptomProductMatch curatedMatch =
        SymptomProductMatch.builder()
            .symptom(cough)
            .productId(coughSyrup.getId())
            .relevanceScore(0.8)
            .matchReason("Curated cough relief")
            .build();

    List<ProductScore> scores =
        ranker.rankSymptomMatches(
            List.of(cough),
            List.of(vitamin, coughSyrup),
            Map.of(coughSyrup.getId(), List.of(curatedMatch)),
            10);

    assertThat(scores).hasSize(1);
    assertThat(scores.get(0).product().getId()).isEqualTo(coughSyrup.getId());
    assertThat(scores.get(0).score()).isGreaterThan(0.5);
    assertThat(scores.get(0).reason())
        .contains("curated symptom match", "metadata overlap", "embedding skipped");
    assertThat(scores.get(0).matchedSymptomIds()).containsExactly(cough.getSymptomId());
  }

  @Test
  void rankSimilarProductsShouldExcludeDismissedProductsAndRankSharedMetadata() {
    ProductSnapshot seed =
        product(
            100L,
            "Pain Relief 500",
            category(20L, "Pain Relief"),
            List.of(substance("Paracetamol")),
            "OTC",
            false);
    ProductSnapshot sameSubstance =
        product(
            101L,
            "Acetaminophen Care",
            category(20L, "Pain Relief"),
            List.of(substance("Paracetamol")),
            "OTC",
            false);
    ProductSnapshot dismissed =
        product(
            102L,
            "Headache Tablets",
            category(20L, "Pain Relief"),
            List.of(substance("Paracetamol")),
            "OTC",
            false);

    List<ProductScore> scores =
        ranker.rankSimilarProducts(
            seed,
            List.of(seed, dismissed, sameSubstance),
            Set.of(sameSubstance.getId()),
            Set.of(dismissed.getId()),
            10);

    assertThat(scores).hasSize(1);
    assertThat(scores.get(0).product().getId()).isEqualTo(sameSubstance.getId());
    assertThat(scores.get(0).reason())
        .contains(
            "embedding skipped",
            "shared substances",
            "same category",
            "positive prior interaction");
  }

  @Test
  void rankAlternativeProductsShouldPreferKnownSubstitutes() {
    ProductSnapshot seed =
        product(
            100L,
            "Pain Relief 500",
            category(20L, "Pain Relief"),
            List.of(substance("Paracetamol")),
            "OTC",
            false);
    ProductSnapshot substitute =
        product(
            101L,
            "Generic Pain Relief",
            category(20L, "Pain Relief"),
            List.of(substance("Paracetamol")),
            "OTC",
            false);
    ProductSubstituteSnapshot relation = new ProductSubstituteSnapshot();
    relation.setOriginalProductId(seed.getId());
    relation.setSubstituteProductId(substitute.getId());
    relation.setIsTherapeuticEquivalent(true);

    List<ProductScore> scores =
        ranker.rankAlternativeProducts(
            seed, List.of(seed, substitute), List.of(relation), Set.of(), Set.of(), 10);

    assertThat(scores).hasSize(1);
    assertThat(scores.get(0).product().getId()).isEqualTo(substitute.getId());
    assertThat(scores.get(0).reason())
        .contains(
            "known product substitute", "shared category or substances", "therapeutic equivalent");
  }

  @Test
  void rankPersonalizedProductsShouldApplyDiversityAfterBaseRanking() {
    Symptom symptom =
        Symptom.builder()
            .symptomId(7L)
            .name("Dry cough")
            .description("Throat irritation")
            .severityLevel(SymptomSeverityLevel.MEDIUM)
            .isActive(true)
            .build();
    CategorySnapshot respiratory = category(30L, "Respiratory Care");
    ProductSnapshot weakerFirst =
        product(301L, "Generic Tablets", respiratory, List.of(), "OTC", false);
    ProductSnapshot strongerSecond =
        product(302L, "Dry Cough Syrup", respiratory, List.of(), "OTC", false);
    strongerSecond.setDescription("Dry cough and throat relief");
    SymptomProductMatch curatedMatch =
        SymptomProductMatch.builder()
            .symptom(symptom)
            .productId(strongerSecond.getId())
            .relevanceScore(1.0)
            .build();

    List<ProductScore> scores =
        ranker.rankPersonalizedProducts(
            List.of(symptom),
            List.of(weakerFirst, strongerSecond),
            Map.of(strongerSecond.getId(), List.of(curatedMatch)),
            Set.of(weakerFirst.getId()),
            Set.of(),
            10);

    assertThat(scores).hasSize(2);
    assertThat(scores.get(0).product().getId()).isEqualTo(strongerSecond.getId());
    assertThat(scores.get(0).reason()).contains("category diversity boost");
    assertThat(scores.get(1).reason()).doesNotContain("category diversity boost");
  }

  @Test
  void rankSimilarProductsShouldNotTreatMissingProductTypesAsCompatibilitySignal() {
    ProductSnapshot seed = product(100L, "Plain Product", null, List.of(), null, false);
    ProductSnapshot candidate = product(101L, "Other Product", null, List.of(), null, false);

    List<ProductScore> scores =
        ranker.rankSimilarProducts(seed, List.of(seed, candidate), Set.of(), Set.of(), 10);

    assertThat(scores).isEmpty();
  }

  private static ProductSnapshot product(
      Long id,
      String name,
      CategorySnapshot category,
      List<SubstanceSnapshot> substances,
      String productType,
      boolean requiresPrescription) {
    ProductSnapshot product = new ProductSnapshot();
    product.setId(id);
    product.setName(name);
    product.setCategory(category);
    product.setSubstances(substances);
    product.setProductType(productType);
    product.setRequiresPrescription(requiresPrescription);
    product.setIsActive(true);
    return product;
  }

  private static CategorySnapshot category(Long id, String name) {
    CategorySnapshot category = new CategorySnapshot();
    category.setId(id);
    category.setName(name);
    return category;
  }

  private static SubstanceSnapshot substance(String inn) {
    SubstanceSnapshot substance = new SubstanceSnapshot();
    substance.setInn(inn);
    return substance;
  }
}
