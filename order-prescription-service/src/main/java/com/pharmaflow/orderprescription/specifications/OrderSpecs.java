package com.pharmaflow.orderprescription.specifications;

import com.pharmaflow.orderprescription.models.Order;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecs {

    public static Specification<Order> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("userId"), userId);
        };
    }

    public static Specification<Order> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }
}
