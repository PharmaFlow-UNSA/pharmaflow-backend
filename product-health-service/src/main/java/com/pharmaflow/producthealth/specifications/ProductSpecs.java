package com.pharmaflow.producthealth.specifications;

import com.pharmaflow.producthealth.models.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecs {

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")),
                          "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Product> manufacturerEquals(String manufacturer) {
        return (root, query, cb) -> {
            if (manufacturer == null || manufacturer.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("manufacturer")),
                            manufacturer.toLowerCase().trim());
        };
    }

    public static Specification<Product> productTypeEquals(Product.ProductType type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("productType"), type);
        };
    }

    public static Specification<Product> requiresPrescription(Boolean requiresPrescription) {
        return (root, query, cb) -> {
            if (requiresPrescription == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("requiresPrescription"), requiresPrescription);
        };
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return cb.conjunction();
            }
            if (minPrice == null) {
                return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
            }
            if (maxPrice == null) {
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            }
            return cb.between(root.get("price"), minPrice, maxPrice);
        };
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("isActive"), true);
    }
}
