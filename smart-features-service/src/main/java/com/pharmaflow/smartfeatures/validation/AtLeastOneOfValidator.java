package com.pharmaflow.smartfeatures.validation;

import java.lang.reflect.Field;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtLeastOneOfValidator implements ConstraintValidator<AtLeastOneOf, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneOf constraintAnnotation) {
        fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        for (String fieldName : fields) {
            Object fieldValue = readField(value, fieldName);
            if (fieldValue instanceof String text && text != null && !text.isBlank()) {
                return true;
            }
            if (!(fieldValue instanceof String) && fieldValue != null) {
                return true;
            }
        }
        return false;
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
