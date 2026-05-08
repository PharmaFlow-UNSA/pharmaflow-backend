package com.pharmaflow.smartfeatures.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

public class ChronologicalRangeValidator
    implements ConstraintValidator<ChronologicalRange, Object> {

  private String startField;
  private String endField;

  @Override
  public void initialize(ChronologicalRange constraintAnnotation) {
    startField = constraintAnnotation.startField();
    endField = constraintAnnotation.endField();
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    Object start = readField(value, startField);
    Object end = readField(value, endField);
    if (start == null || end == null) {
      return true;
    }

    if (!(start instanceof Comparable) || !(end instanceof Comparable)) {
      return false;
    }

    Comparable<Object> comparableStart = (Comparable<Object>) start;
    return comparableStart.compareTo(end) <= 0;
  }

  private Object readField(Object target, String fieldName) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException ex) {
      return null;
    }
  }
}
