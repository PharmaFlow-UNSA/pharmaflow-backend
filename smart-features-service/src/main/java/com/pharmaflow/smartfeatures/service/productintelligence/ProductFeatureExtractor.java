package com.pharmaflow.smartfeatures.service.productintelligence;

import com.pharmaflow.smartfeatures.dto.product.CategorySnapshot;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.SubstanceSnapshot;
import com.pharmaflow.smartfeatures.model.symptom.Symptom;
import com.pharmaflow.smartfeatures.util.TextSanitizer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProductFeatureExtractor {

  public ProductFeatures extract(ProductSnapshot product) {
    Set<String> allTokens = new LinkedHashSet<>();
    Set<String> categoryTokens = new LinkedHashSet<>();
    Set<String> substanceTokens = new LinkedHashSet<>();

    addTokens(allTokens, product.getName());
    addTokens(allTokens, product.getBrandName());
    addTokens(allTokens, product.getDescription());
    addTokens(allTokens, product.getProductType());
    addTokens(allTokens, product.getManufacturer());
    addTokens(allTokens, product.getPackageSize());

    CategorySnapshot category = product.getCategory();
    if (category != null) {
      addTokens(categoryTokens, category.getName());
      addTokens(categoryTokens, category.getDescription());
      allTokens.addAll(categoryTokens);
    }

    for (SubstanceSnapshot substance :
        product.getSubstances() == null ? List.<SubstanceSnapshot>of() : product.getSubstances()) {
      addTokens(substanceTokens, substance.getInn());
      addTokens(substanceTokens, substance.getCommonName());
      addTokens(substanceTokens, substance.getAtcCode());
      addTokens(substanceTokens, substance.getDescription());
    }
    allTokens.addAll(substanceTokens);

    if (Boolean.TRUE.equals(product.getRequiresPrescription())) {
      allTokens.add("prescription");
      allTokens.add("controlled");
    }

    String embeddingText =
        String.join(
            " ",
            nonNullValues(
                product.getName(),
                product.getBrandName(),
                product.getProductType(),
                category != null ? category.getName() : null,
                product.getDescription(),
                product.getPackageSize(),
                product.getManufacturer(),
                String.join(" ", substanceTokens)));

    return new ProductFeatures(
        allTokens,
        categoryTokens,
        substanceTokens,
        normalizeKeyword(product.getProductType()),
        Boolean.TRUE.equals(product.getRequiresPrescription()),
        embeddingText);
  }

  public Set<String> symptomTokens(Collection<Symptom> symptoms) {
    Set<String> tokens = new LinkedHashSet<>();
    for (Symptom symptom : symptoms) {
      addTokens(tokens, symptom.getName());
      addTokens(tokens, symptom.getDescription());
      for (String tag : symptom.getTags() == null ? List.<String>of() : symptom.getTags()) {
        addTokens(tokens, tag);
      }
    }
    return tokens;
  }

  public Set<String> tokens(String value) {
    Set<String> tokens = new LinkedHashSet<>();
    addTokens(tokens, value);
    return tokens;
  }

  private void addTokens(Set<String> tokens, String value) {
    String sanitized = TextSanitizer.sanitizeOptionalText(value);
    if (sanitized == null) {
      return;
    }
    for (String token : sanitized.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
      String normalized = normalizeKeyword(token);
      if (normalized != null && normalized.length() >= 2) {
        tokens.add(normalized);
      }
    }
  }

  private List<String> nonNullValues(String... values) {
    return java.util.Arrays.stream(values)
        .map(TextSanitizer::sanitizeOptionalText)
        .filter(value -> value != null && !value.isBlank())
        .toList();
  }

  private String normalizeKeyword(String value) {
    String sanitized = TextSanitizer.sanitizeOptionalText(value);
    return sanitized == null ? null : sanitized.toLowerCase(Locale.ROOT);
  }
}
