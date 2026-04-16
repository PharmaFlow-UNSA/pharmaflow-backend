package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.ProductSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSubstituteRepository extends JpaRepository<ProductSubstitute, Long> {

    @Query("SELECT ps FROM ProductSubstitute ps JOIN FETCH ps.originalProduct JOIN FETCH ps.substituteProduct")
    List<ProductSubstitute> findAllWithProducts();

    @Query("SELECT ps FROM ProductSubstitute ps JOIN FETCH ps.originalProduct JOIN FETCH ps.substituteProduct " +
           "WHERE ps.originalProduct.id = :productId")
    List<ProductSubstitute> findByOriginalProductIdWithDetails(@Param("productId") Long productId);

    boolean existsByOriginalProductIdAndSubstituteProductId(Long originalId, Long substituteId);
}
