package com.pharmaflow.orderprescription.specifications;

import com.pharmaflow.orderprescription.models.Payment;
import org.springframework.data.jpa.domain.Specification;

public class PaymentSpecs {

    public static Specification<Payment> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }

    public static Specification<Payment> hasMethod(String method) {
        return (root, query, cb) -> {
            if (method == null || method.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("method"), method.trim().toUpperCase());
        };
    }
}
