package com.pharmaflow.userhealth.specifications;

import com.pharmaflow.userhealth.models.Therapy;
import org.springframework.data.jpa.domain.Specification;

public class TherapySpecs {

    public static Specification<Therapy> medicationNameContains(String medicationName) {
        return (root, query, cb) -> {
            if (medicationName == null || medicationName.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("medicationName")),
                          "%" + medicationName.toLowerCase().trim() + "%");
        };
    }
}

