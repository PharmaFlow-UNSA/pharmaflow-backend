package com.pharmaflow.pharmacyinventory.specifications;

import com.pharmaflow.pharmacyinventory.models.Inventory;
import org.springframework.data.jpa.domain.Specification;

public class InventorySpecs {

    public static Specification<Inventory> hasPharmacyId(Long pharmacyId) {
        return (root, query, cb) -> {
            if (pharmacyId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("pharmacy").get("id"), pharmacyId);
        };
    }

    public static Specification<Inventory> hasProductId(Long productId) {
        return (root, query, cb) -> {
            if (productId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("productId"), productId);
        };
    }

    public static Specification<Inventory> quantityAtLeast(Integer minQuantity) {
        return (root, query, cb) -> {
            if (minQuantity == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("quantity"), minQuantity);
        };
    }
}
