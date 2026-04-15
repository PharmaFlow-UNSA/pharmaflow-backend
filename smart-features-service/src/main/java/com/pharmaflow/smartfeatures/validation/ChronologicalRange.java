package com.pharmaflow.smartfeatures.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ChronologicalRangeValidator.class)
public @interface ChronologicalRange {

    String message() default "End value must not be before start value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String startField();

    String endField();
}
