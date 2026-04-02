package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.Contraindication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContraindicationRepository extends JpaRepository<Contraindication, Long> {

    List<Contraindication> findBySubstanceId(Long substanceId);

    List<Contraindication> findByType(Contraindication.ContraindicationType type);
}