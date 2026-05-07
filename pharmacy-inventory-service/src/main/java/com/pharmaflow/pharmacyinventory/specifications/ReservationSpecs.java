package com.pharmaflow.pharmacyinventory.specifications;

import com.pharmaflow.pharmacyinventory.models.Reservation;
import org.springframework.data.jpa.domain.Specification;

public class ReservationSpecs {

    public static Specification<Reservation> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }

    public static Specification<Reservation> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("userId"), userId);
        };
    }

    public static Specification<Reservation> hasPharmacyId(Long pharmacyId) {
        return (root, query, cb) -> {
            if (pharmacyId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("pharmacy").get("id"), pharmacyId);
        };
    }
}
