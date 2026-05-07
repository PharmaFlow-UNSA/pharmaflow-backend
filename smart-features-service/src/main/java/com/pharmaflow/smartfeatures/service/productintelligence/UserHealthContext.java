package com.pharmaflow.smartfeatures.service.productintelligence;

import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.SubstanceSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

public class UserHealthContext {

  private static final UserHealthContext EMPTY = new UserHealthContext(Set.of());

  private final Set<String> allergyTerms;

  private UserHealthContext(Set<String> allergyTerms) {
    this.allergyTerms = allergyTerms;
  }

  public static UserHealthContext empty() {
    return EMPTY;
  }

  public static UserHealthContext from(PatientHealthProfileSnapshot profile) {
    if (profile == null || profile.getAllergies() == null || profile.getAllergies().isEmpty()) {
      return empty();
    }

    Set<String> allergyTerms =
        profile.getAllergies().stream()
            .filter(Objects::nonNull)
            .flatMap(allergy -> Stream.of(allergy.getActiveSubstance(), allergy.getAllergen()))
            .map(UserHealthContext::normalize)
            .filter(term -> term.length() >= 3)
            .collect(Collectors.toSet());
    return allergyTerms.isEmpty() ? empty() : new UserHealthContext(allergyTerms);
  }

  public List<ProductSnapshot> withoutAllergyConflicts(Collection<ProductSnapshot> products) {
    if (products == null || products.isEmpty()) {
      return List.of();
    }
    if (allergyTerms.isEmpty()) {
      return List.copyOf(products);
    }

    return products.stream().filter(product -> !hasAllergyConflict(product)).toList();
  }

  public boolean hasAllergyConflict(ProductSnapshot product) {
    if (product == null || allergyTerms.isEmpty()) {
      return false;
    }

    String productText = productText(product);
    return allergyTerms.stream().anyMatch(term -> containsTerm(productText, term));
  }

  private String productText(ProductSnapshot product) {
    Stream<String> productFields =
        Stream.of(
            product.getName(),
            product.getDescription(),
            product.getBrandName(),
            product.getManufacturer());
    Stream<String> substanceFields =
        product.getSubstances() == null
            ? Stream.empty()
            : product.getSubstances().stream()
                .filter(Objects::nonNull)
                .flatMap(UserHealthContext::substanceFields);

    return Stream.concat(productFields, substanceFields)
        .map(UserHealthContext::normalize)
        .filter(StringUtils::hasText)
        .collect(Collectors.joining(" "));
  }

  private static Stream<String> substanceFields(SubstanceSnapshot substance) {
    return Stream.of(
        substance.getInn(),
        substance.getCommonName(),
        substance.getAtcCode(),
        substance.getDescription());
  }

  private static boolean containsTerm(String text, String term) {
    return StringUtils.hasText(text) && (" " + text + " ").contains(" " + term + " ");
  }

  private static String normalize(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String withoutAccents =
        Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }
}
