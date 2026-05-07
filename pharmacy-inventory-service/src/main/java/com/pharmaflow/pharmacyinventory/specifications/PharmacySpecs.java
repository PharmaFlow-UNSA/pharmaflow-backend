package com.pharmaflow.pharmacyinventory.specifications;

import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import org.springframework.data.jpa.domain.Specification;

public class PharmacySpecs {

    public static Specification<Pharmacy> nameContains(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")),
                    "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Pharmacy> cityEquals(String city) {
        return (root, query, cb) -> {
            if (city == null || city.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("city")), city.toLowerCase().trim());
        };
    }
}
