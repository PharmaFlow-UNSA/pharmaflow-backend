package com.pharmaflow.smartfeatures.util;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SymptomTextNormalizer {

  private SymptomTextNormalizer() {}

  public static String sanitizeName(String value) {
    return TextSanitizer.sanitizeRequiredText(value);
  }

  public static String sanitizeDescription(String value) {
    return TextSanitizer.sanitizeOptionalText(value);
  }

  public static String sanitizeQuery(String value) {
    return TextSanitizer.sanitizeRequiredText(value);
  }

  public static String normalizeName(String value) {
    String sanitizedName = sanitizeName(value);
    return sanitizedName == null ? null : sanitizedName.toLowerCase(Locale.ROOT);
  }

  public static String collapseWhitespace(String value) {
    return TextSanitizer.sanitizeText(value);
  }

  public static List<String> sanitizeTags(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .map(TextSanitizer::sanitizeOptionalText)
        .filter(Objects::nonNull)
        .map(value -> value.toLowerCase(Locale.ROOT))
        .distinct()
        .sorted()
        .toList();
  }
}
