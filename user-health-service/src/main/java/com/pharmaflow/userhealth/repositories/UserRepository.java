package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository has methods: save(), findAll(), delete(), etc.
}