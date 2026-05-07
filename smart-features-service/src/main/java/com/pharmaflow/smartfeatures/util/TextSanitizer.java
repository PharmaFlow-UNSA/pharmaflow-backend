package com.pharmaflow.smartfeatures.util;

import java.util.Locale;

public final class TextSanitizer {

  private TextSanitizer() {}

  public static String sanitizeText(String value) {
    if (value == null) {
      return null;
    }

    String collapsed = value.trim().replaceAll("\\s+", " ");
    return collapsed.isEmpty() ? null : collapsed;
  }

  public static String sanitizeRequiredText(String value) {
    return sanitizeText(value);
  }

  public static String sanitizeOptionalText(String value) {
    return sanitizeText(value);
  }

  public static String normalizeKey(String value) {
    String sanitized = sanitizeText(value);
    return sanitized == null ? null : sanitized.toLowerCase(Locale.ROOT);
  }
}
