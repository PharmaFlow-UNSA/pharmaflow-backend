package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.Substance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubstanceRepository extends JpaRepository<Substance, Long> {

    Optional<Substance> findByInn(String inn);

    boolean existsByInn(String inn);
}