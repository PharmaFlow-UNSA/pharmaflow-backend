package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, 
                                       JpaSpecificationExecutor<User> {

    @NotNull
    @EntityGraph(attributePaths = {"patientProfile", "patientProfile.allergies", "patientProfile.therapies", "familyMembers"})
    Optional<User> findById(@NotNull Long id);

    boolean existsByEmail(String email);
}