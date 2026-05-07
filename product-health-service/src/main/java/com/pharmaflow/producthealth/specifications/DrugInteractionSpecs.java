package com.pharmaflow.producthealth.specifications;

import com.pharmaflow.producthealth.models.DrugInteraction;
import org.springframework.data.jpa.domain.Specification;

public class DrugInteractionSpecs {

    public static Specification<DrugInteraction> hasSeverity(DrugInteraction.SeverityLevel severity) {
        return (root, query, cb) -> {
            if (severity == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("severity"), severity);
        };
    }

    public static Specification<DrugInteraction> substanceNameContains(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + name.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.join("substanceA").get("commonName")), pattern),
                cb.like(cb.lower(root.join("substanceB").get("commonName")), pattern)
            );
        };
    }
}
