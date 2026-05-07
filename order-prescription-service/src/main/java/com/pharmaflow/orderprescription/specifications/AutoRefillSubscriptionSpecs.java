package com.pharmaflow.orderprescription.specifications;

import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import org.springframework.data.jpa.domain.Specification;

public class AutoRefillSubscriptionSpecs {

    public static Specification<AutoRefillSubscription> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("userId"), userId);
        };
    }

    public static Specification<AutoRefillSubscription> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }

    public static Specification<AutoRefillSubscription> hasProductId(Long productId) {
        return (root, query, cb) -> {
            if (productId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("productId"), productId);
        };
    }
}
