package com.pharmaflow.pharmacyinventory.specifications;

import com.pharmaflow.pharmacyinventory.models.Delivery;
import org.springframework.data.jpa.domain.Specification;

public class DeliverySpecs {

    public static Specification<Delivery> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }

    public static Specification<Delivery> hasOrderId(Long orderId) {
        return (root, query, cb) -> {
            if (orderId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("orderId"), orderId);
        };
    }

    public static Specification<Delivery> hasPharmacyId(Long pharmacyId) {
        return (root, query, cb) -> {
            if (pharmacyId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("pharmacy").get("id"), pharmacyId);
        };
    }
}
