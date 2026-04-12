package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.Contraindication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContraindicationRepository extends JpaRepository<Contraindication, Long> {

    @Query("SELECT c FROM Contraindication c JOIN FETCH c.substance")
    List<Contraindication> findAllWithSubstance();

    @Query("SELECT c FROM Contraindication c JOIN FETCH c.substance WHERE c.substance.id = :substanceId")
    List<Contraindication> findBySubstanceIdWithDetails(@Param("substanceId") Long substanceId);
}
