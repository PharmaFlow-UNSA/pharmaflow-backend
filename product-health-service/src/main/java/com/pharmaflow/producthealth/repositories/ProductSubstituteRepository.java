package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.ProductSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubstituteRepository extends JpaRepository<ProductSubstitute, Long> {

    List<ProductSubstitute> findByOriginalProductId(Long originalProductId);

    List<ProductSubstitute> findBySubstituteType(ProductSubstitute.SubstituteType type);
}