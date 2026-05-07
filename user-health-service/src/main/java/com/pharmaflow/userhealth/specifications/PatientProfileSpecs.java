package com.pharmaflow.userhealth.specifications;

import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.models.enums.BloodType;
import org.springframework.data.jpa.domain.Specification;

public class PatientProfileSpecs {

    public static Specification<PatientProfile> hasBloodType(BloodType bloodType) {
        return (root, query, cb) -> {
            if (bloodType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("bloodType"), bloodType);
        };
    }

    public static Specification<PatientProfile> bmiBetween(Double minBMI, Double maxBMI) {
        return (root, query, cb) -> {
            if (minBMI == null && maxBMI == null) {
                return cb.conjunction();
            }
            
            // Calculate BMI: weight / (height * height)
            var weight = root.<Double>get("weight");
            var height = root.<Double>get("height");
            var heightSquared = cb.prod(height, height);
            var bmi = cb.quot(weight, heightSquared);

            if (minBMI != null && maxBMI != null) {
                return cb.and(cb.ge(bmi, minBMI), cb.le(bmi, maxBMI));
            } else if (minBMI != null) {
                return cb.ge(bmi, minBMI);
            } else {
                return cb.le(bmi, maxBMI);
            }
        };
    }
}

