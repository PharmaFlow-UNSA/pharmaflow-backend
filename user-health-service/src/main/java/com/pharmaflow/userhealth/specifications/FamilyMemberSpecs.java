package com.pharmaflow.userhealth.specifications;

import com.pharmaflow.userhealth.models.FamilyMember;
import com.pharmaflow.userhealth.models.enums.Relationship;
import org.springframework.data.jpa.domain.Specification;

public class FamilyMemberSpecs {

    public static Specification<FamilyMember> hasRelationship(Relationship relationship) {
        return (root, query, cb) -> {
            if (relationship == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("relationship"), relationship);
        };
    }
}

