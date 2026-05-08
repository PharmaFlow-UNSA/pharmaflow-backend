package com.pharmaflow.smartfeatures.service.productintelligence;

import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.ProductSubstituteSnapshot;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.model.symptom.SymptomProductMatch;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductRanker {

  private static final int DEFAULT_LIMIT = 10;

  private final ProductFeatureExtractor featureExtractor;
  private final EmbeddingSimilarityService embeddingSimilarityService;

  public ProductRanker(
      ProductFeatureExtractor featureExtractor,
      EmbeddingSimilarityService embeddingSimilarityService) {
    this.featureExtractor = featureExtractor;
    this.embeddingSimilarityService = embeddingSimilarityService;
  }

  public List<ProductScore> rankSymptomMatches(
      Collection<Symptom> symptoms,
      List<ProductSnapshot> products,
      Map<Long, List<SymptomProductMatch>> curatedMatches,
      int limit) {
    Set<String> symptomTokens = featureExtractor.symptomTokens(symptoms);
    String symptomText =
        symptoms.stream()
            .flatMap(
                symptom -> java.util.stream.Stream.of(symptom.getName(), symptom.getDescription()))
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "));

    return products.stream()
        .filter(this::isActiveProduct)
        .map(
            product ->
                scoreSymptomProduct(product, symptoms, symptomTokens, symptomText, curatedMatches))
        .filter(score -> score.score() > 0.0)
        .sorted(scoreComparator())
        .limit(normalizedLimit(limit))
        .toList();
  }

  public List<ProductScore> rankSimilarProducts(
      ProductSnapshot seedProduct,
      List<ProductSnapshot> products,
      Set<Long> positiveProductIds,
      Set<Long> dismissedProductIds,
      int limit) {
    ProductFeatures seedFeatures = featureExtractor.extract(seedProduct);
    return products.stream()
        .filter(this::isActiveProduct)
        .filter(product -> !Objects.equals(product.getId(), seedProduct.getId()))
        .filter(product -> !dismissedProductIds.contains(product.getId()))
        .map(product -> scoreSimilarProduct(seedProduct, seedFeatures, product, positiveProductIds))
        .filter(score -> score.score() > 0.0)
        .sorted(scoreComparator())
        .limit(normalizedLimit(limit))
        .toList();
  }

  public List<ProductScore> rankAlternativeProducts(
      ProductSnapshot seedProduct,
      List<ProductSnapshot> products,
      List<ProductSubstituteSnapshot> substitutes,
      Set<Long> positiveProductIds,
      Set<Long> dismissedProductIds,
      int limit) {
    Map<Long, ProductSubstituteSnapshot> substitutesByProduct =
        substitutes.stream()
            .filter(substitute -> substitute.getSubstituteProductId() != null)
            .collect(
                Collectors.toMap(
                    ProductSubstituteSnapshot::getSubstituteProductId,
                    Function.identity(),
                    (existing, replacement) -> existing,
                    LinkedHashMap::new));
    ProductFeatures seedFeatures = featureExtractor.extract(seedProduct);

    return products.stream()
        .filter(this::isActiveProduct)
        .filter(product -> !Objects.equals(product.getId(), seedProduct.getId()))
        .filter(product -> !dismissedProductIds.contains(product.getId()))
        .map(
            product ->
                scoreAlternativeProduct(
                    seedProduct,
                    seedFeatures,
                    product,
                    substitutesByProduct.get(product.getId()),
                    positiveProductIds))
        .filter(score -> score.score() > 0.0)
        .sorted(scoreComparator())
        .limit(normalizedLimit(limit))
        .toList();
  }

  public List<ProductScore> rankPersonalizedProducts(
      Collection<Symptom> symptoms,
      List<ProductSnapshot> products,
      Map<Long, List<SymptomProductMatch>> curatedMatches,
      Set<Long> positiveProductIds,
      Set<Long> dismissedProductIds,
      int limit) {
    List<ProductScore> symptomScores =
        rankSymptomMatches(
            symptoms, products, curatedMatches, Math.max(products.size(), normalizedLimit(limit)));
    Map<Long, ProductScore> symptomScoreByProduct =
        symptomScores.stream()
            .collect(Collectors.toMap(score -> score.product().getId(), Function.identity()));
    Set<Long> seenCategories = new HashSet<>();

    List<ProductScore> baseScores =
        products.stream()
            .filter(this::isActiveProduct)
            .filter(product -> !dismissedProductIds.contains(product.getId()))
            .map(
                product -> {
                  ProductScore symptomScore = symptomScoreByProduct.get(product.getId());
                  double symptomContribution =
                      symptomScore == null ? 0.0 : symptomScore.score() * 0.5;
                  double interactionContribution =
                      positiveProductIds.contains(product.getId()) ? 0.2 : 0.0;
                  double score = clamp(symptomContribution + interactionContribution);
                  String reason =
                      appendReason(
                          symptomScore == null ? null : symptomScore.reason(),
                          interactionContribution > 0 ? "positive prior interaction" : null);
                  return new ProductScore(
                      product,
                      score,
                      reason,
                      symptomScore == null ? List.of() : symptomScore.matchedSymptomIds());
                })
            .filter(score -> score.score() > 0.0)
            .sorted(scoreComparator())
            .toList();

    return baseScores.stream()
        .map(
            score -> {
              Long categoryId =
                  score.product().getCategory() == null
                      ? null
                      : score.product().getCategory().getId();
              boolean diversityBoost = categoryId != null && seenCategories.add(categoryId);
              if (!diversityBoost) {
                return score;
              }
              return new ProductScore(
                  score.product(),
                  clamp(score.score() + 0.1),
                  appendReason(score.reason(), "category diversity boost"),
                  score.matchedSymptomIds());
            })
        .sorted(scoreComparator())
        .limit(normalizedLimit(limit))
        .toList();
  }

  private ProductScore scoreSymptomProduct(
      ProductSnapshot product,
      Collection<Symptom> symptoms,
      Set<String> symptomTokens,
      String symptomText,
      Map<Long, List<SymptomProductMatch>> curatedMatches) {
    ProductFeatures features = featureExtractor.extract(product);
    List<SymptomProductMatch> curated = curatedMatches.getOrDefault(product.getId(), List.of());
    double curatedScore =
        curated.stream()
            .map(SymptomProductMatch::getRelevanceScore)
            .filter(Objects::nonNull)
            .reduce(0.0, Double::sum);
    curatedScore = Math.min(curatedScore, 1.0);
    double tokenScore = overlap(symptomTokens, features.tokens());
    double categoryScore =
        overlap(symptomTokens, features.categoryTokens()) * 0.5
            + overlap(symptomTokens, features.substanceTokens()) * 0.5;
    OptionalDouble embeddingScore =
        embeddingSimilarityService.similarity(symptomText, features.embeddingText());

    double score =
        weightedScore(
            List.of(
                new WeightedSignal(curatedScore, 0.4),
                new WeightedSignal(tokenScore, 0.3),
                new WeightedSignal(categoryScore, 0.2),
                embeddingScore.isPresent()
                    ? new WeightedSignal(embeddingScore.getAsDouble(), 0.1)
                    : WeightedSignal.disabled(0.1)));

    List<Long> matchedSymptomIds =
        curated.stream()
            .map(match -> match.getSymptom().getSymptomId())
            .distinct()
            .sorted()
            .toList();
    if (matchedSymptomIds.isEmpty()) {
      matchedSymptomIds =
          symptoms.stream().map(Symptom::getSymptomId).filter(Objects::nonNull).sorted().toList();
    }
    return new ProductScore(
        product,
        clamp(score),
        appendReason(
            curatedScore > 0 ? "curated symptom match" : null,
            tokenScore > 0 ? "symptom/product metadata overlap" : null,
            categoryScore > 0 ? "category or substance relevance" : null,
            embeddingScore.isPresent() ? "embedding similarity" : "embedding skipped"),
        matchedSymptomIds);
  }

  private ProductScore scoreSimilarProduct(
      ProductSnapshot seedProduct,
      ProductFeatures seedFeatures,
      ProductSnapshot product,
      Set<Long> positiveProductIds) {
    ProductFeatures features = featureExtractor.extract(product);
    OptionalDouble embeddingScore =
        embeddingSimilarityService.similarity(
            seedFeatures.embeddingText(), features.embeddingText());
    double sharedSubstanceScore =
        overlap(seedFeatures.substanceTokens(), features.substanceTokens());
    double categoryScore = sameCategory(seedProduct, product) ? 1.0 : 0.0;
    double compatibilityScore =
        seedFeatures.productType() != null
                && features.productType() != null
                && Objects.equals(seedFeatures.productType(), features.productType())
                && seedFeatures.requiresPrescription() == features.requiresPrescription()
            ? 1.0
            : 0.0;
    double interactionScore = positiveProductIds.contains(product.getId()) ? 1.0 : 0.0;

    double score =
        weightedScore(
            List.of(
                embeddingScore.isPresent()
                    ? new WeightedSignal(embeddingScore.getAsDouble(), 0.45)
                    : WeightedSignal.disabled(0.45),
                new WeightedSignal(sharedSubstanceScore, 0.25),
                new WeightedSignal(categoryScore, 0.15),
                new WeightedSignal(compatibilityScore, 0.10),
                new WeightedSignal(interactionScore, 0.05)));
    return new ProductScore(
        product,
        clamp(score),
        appendReason(
            embeddingScore.isPresent() ? "embedding similarity" : "embedding skipped",
            sharedSubstanceScore > 0 ? "shared substances" : null,
            categoryScore > 0 ? "same category" : null,
            compatibilityScore > 0 ? "compatible product type" : null,
            interactionScore > 0 ? "positive prior interaction" : null),
        List.of());
  }

  private ProductScore scoreAlternativeProduct(
      ProductSnapshot seedProduct,
      ProductFeatures seedFeatures,
      ProductSnapshot product,
      ProductSubstituteSnapshot substitute,
      Set<Long> positiveProductIds) {
    ProductFeatures features = featureExtractor.extract(product);
    OptionalDouble embeddingScore =
        embeddingSimilarityService.similarity(
            seedFeatures.embeddingText(), features.embeddingText());
    double substituteScore = substitute == null ? 0.0 : 1.0;
    double sharedMetadataScore =
        Math.max(
            overlap(seedFeatures.substanceTokens(), features.substanceTokens()),
            sameCategory(seedProduct, product) ? 1.0 : 0.0);
    double therapeuticOrPriceScore =
        substitute != null && Boolean.TRUE.equals(substitute.getIsTherapeuticEquivalent())
            ? 1.0
            : 0.0;
    double interactionScore = positiveProductIds.contains(product.getId()) ? 0.05 : 0.0;

    double score =
        weightedScore(
                List.of(
                    new WeightedSignal(substituteScore, 0.50),
                    new WeightedSignal(sharedMetadataScore, 0.30),
                    embeddingScore.isPresent()
                        ? new WeightedSignal(embeddingScore.getAsDouble(), 0.15)
                        : WeightedSignal.disabled(0.15),
                    new WeightedSignal(therapeuticOrPriceScore, 0.05)))
            + interactionScore;
    return new ProductScore(
        product,
        clamp(score),
        appendReason(
            substituteScore > 0 ? "known product substitute" : null,
            sharedMetadataScore > 0 ? "shared category or substances" : null,
            embeddingScore.isPresent() ? "embedding similarity" : "embedding skipped",
            therapeuticOrPriceScore > 0 ? "therapeutic equivalent" : null,
            interactionScore > 0 ? "positive prior interaction" : null),
        List.of());
  }

  private double weightedScore(List<WeightedSignal> signals) {
    double activeWeight =
        signals.stream().filter(WeightedSignal::active).mapToDouble(WeightedSignal::weight).sum();
    if (activeWeight == 0.0) {
      return 0.0;
    }
    return signals.stream()
        .filter(WeightedSignal::active)
        .mapToDouble(signal -> signal.value() * (signal.weight() / activeWeight))
        .sum();
  }

  private double overlap(Set<String> left, Set<String> right) {
    if (left.isEmpty() || right.isEmpty()) {
      return 0.0;
    }
    long matches = left.stream().filter(right::contains).count();
    return Math.min(1.0, (double) matches / Math.max(1, left.size()));
  }

  private boolean sameCategory(ProductSnapshot left, ProductSnapshot right) {
    return left.getCategory() != null
        && right.getCategory() != null
        && Objects.equals(left.getCategory().getId(), right.getCategory().getId());
  }

  private boolean isActiveProduct(ProductSnapshot product) {
    return product != null
        && product.getId() != null
        && !Boolean.FALSE.equals(product.getIsActive());
  }

  private Comparator<ProductScore> scoreComparator() {
    return Comparator.comparing(ProductScore::score, Comparator.reverseOrder())
        .thenComparing(score -> score.product().getId());
  }

  private double clamp(double score) {
    return Math.max(0.0, Math.min(1.0, score));
  }

  private int normalizedLimit(int limit) {
    return limit > 0 ? limit : DEFAULT_LIMIT;
  }

  private String appendReason(String... parts) {
    return java.util.Arrays.stream(parts)
        .filter(Objects::nonNull)
        .filter(part -> !part.isBlank())
        .distinct()
        .collect(Collectors.joining(" | "));
  }

  private record WeightedSignal(double value, double weight, boolean active) {
    private WeightedSignal(double value, double weight) {
      this(value, weight, true);
    }

    private static WeightedSignal disabled(double weight) {
      return new WeightedSignal(0.0, weight, false);
    }
  }
}
