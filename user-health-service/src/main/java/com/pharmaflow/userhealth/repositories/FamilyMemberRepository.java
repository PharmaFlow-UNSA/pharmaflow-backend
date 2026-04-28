package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    
    @Query("SELECT fm FROM FamilyMember fm LEFT JOIN FETCH fm.patientProfile WHERE fm.user.id = :userId")
    List<FamilyMember> findByUserId(@Param("userId") Long userId);

    @Query("SELECT fm FROM FamilyMember fm WHERE LOWER(fm.relationship) = LOWER(:relationship)")
    List<FamilyMember> findByRelationshipIgnoreCase(@Param("relationship") String relationship);
}

