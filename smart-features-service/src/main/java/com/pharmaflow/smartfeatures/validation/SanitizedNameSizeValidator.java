package com.pharmaflow.smartfeatures.validation;

import com.pharmaflow.smartfeatures.util.TextSanitizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SanitizedNameSizeValidator implements ConstraintValidator<SanitizedNameSize, String> {

    private int min;
    private int max;

    @Override
    public void initialize(SanitizedNameSize constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String sanitizedName = TextSanitizer.sanitizeRequiredText(value);
        return sanitizedName != null && sanitizedName.length() >= min && sanitizedName.length() <= max;
    }
}
