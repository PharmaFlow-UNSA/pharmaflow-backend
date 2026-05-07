package com.pharmaflow.userhealth.specifications;

import com.pharmaflow.userhealth.models.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecs {

    public static Specification<User> emailDomainEquals(String emailDomain) {
        return (root, query, cb) -> {
            if (emailDomain == null || emailDomain.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("email")), 
                          "%@" + emailDomain.toLowerCase().trim());
        };
    }
}

