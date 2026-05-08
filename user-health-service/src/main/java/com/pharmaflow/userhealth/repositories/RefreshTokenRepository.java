package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUserId(Long userId);
    
    void deleteByUserId(Long userId);
    
    void deleteByExpiresAtBefore(LocalDateTime now);
}

