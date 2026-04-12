package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {

    // JOIN FETCH oba substanca - sprječava N+1
    @Query("SELECT d FROM DrugInteraction d JOIN FETCH d.substanceA JOIN FETCH d.substanceB")
    List<DrugInteraction> findAllWithSubstances();

    @Query("SELECT d FROM DrugInteraction d JOIN FETCH d.substanceA JOIN FETCH d.substanceB " +
           "WHERE d.substanceA.id = :substanceId OR d.substanceB.id = :substanceId")
    List<DrugInteraction> findAllBySubstanceId(@Param("substanceId") Long substanceId);

    @Query("SELECT d FROM DrugInteraction d JOIN FETCH d.substanceA JOIN FETCH d.substanceB WHERE " +
           "(d.substanceA.id = :idA AND d.substanceB.id = :idB) OR " +
           "(d.substanceA.id = :idB AND d.substanceB.id = :idA)")
    List<DrugInteraction> findInteractionBetween(@Param("idA") Long idA, @Param("idB") Long idB);

    boolean existsBySubstanceAIdAndSubstanceBId(Long substanceAId, Long substanceBId);
}
