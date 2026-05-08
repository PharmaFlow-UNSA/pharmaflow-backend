package com.pharmaflow.smartfeatures.validation;

import com.pharmaflow.smartfeatures.util.TextSanitizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SanitizedTextSizeValidator implements ConstraintValidator<SanitizedTextSize, String> {

  private int min;
  private int max;
  private boolean allowBlank;

  @Override
  public void initialize(SanitizedTextSize constraintAnnotation) {
    min = constraintAnnotation.min();
    max = constraintAnnotation.max();
    allowBlank = constraintAnnotation.allowBlank();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    String sanitized = TextSanitizer.sanitizeText(value);
    if (sanitized == null) {
      return allowBlank;
    }

    return sanitized.length() >= min && sanitized.length() <= max;
  }
}
