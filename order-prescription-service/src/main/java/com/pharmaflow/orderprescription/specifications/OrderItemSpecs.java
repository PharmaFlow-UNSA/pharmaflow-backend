package com.pharmaflow.orderprescription.specifications;

import com.pharmaflow.orderprescription.models.OrderItem;
import org.springframework.data.jpa.domain.Specification;

public class OrderItemSpecs {

    public static Specification<OrderItem> hasOrderId(Long orderId) {
        return (root, query, cb) -> {
            if (orderId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("order").get("id"), orderId);
        };
    }

    public static Specification<OrderItem> hasProductId(Long productId) {
        return (root, query, cb) -> {
            if (productId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("productId"), productId);
        };
    }
}
