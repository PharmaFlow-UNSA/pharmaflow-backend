package com.pharmaflow.smartfeatures.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NullablePositiveValidator implements ConstraintValidator<NullablePositive, Number> {

  @Override
  public boolean isValid(Number value, ConstraintValidatorContext context) {
    return value == null || value.longValue() > 0;
  }
}
