package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long>, 
                                          JpaSpecificationExecutor<Allergy> {
}

