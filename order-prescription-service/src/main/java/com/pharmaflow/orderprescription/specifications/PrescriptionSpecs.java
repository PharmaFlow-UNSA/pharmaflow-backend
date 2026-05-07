package com.pharmaflow.orderprescription.specifications;

import com.pharmaflow.orderprescription.models.Prescription;
import org.springframework.data.jpa.domain.Specification;

public class PrescriptionSpecs {

    public static Specification<Prescription> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("userId"), userId);
        };
    }

    public static Specification<Prescription> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }
}
