package com.pharmaflow.userhealth.specifications;
import com.pharmaflow.userhealth.models.Allergy;
import com.pharmaflow.userhealth.models.enums.Severity;
import org.springframework.data.jpa.domain.Specification;

public class AllergySpecs {

    public static Specification<Allergy> hasSeverity(Severity severity) {
        return (root, query, cb) -> {
            if (severity == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("severity"), severity);
        };
    }

    public static Specification<Allergy> allergenContains(String allergen) {
        return (root, query, cb) -> {
            if (allergen == null || allergen.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("allergen")), 
                          "%" + allergen.toLowerCase().trim() + "%");
        };
    }
}
