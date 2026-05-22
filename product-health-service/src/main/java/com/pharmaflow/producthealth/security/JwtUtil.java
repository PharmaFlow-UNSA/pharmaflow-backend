package com.pharmaflow.producthealth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * JWT utility for Product & Medical Service.
 * Validates tokens issued by user-health-service using the same shared secret.
 * This service does NOT issue tokens — authentication is centralized.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:pharmaflow-secret-key-2024-very-long-and-secure-key-for-production}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Validate token and return claims.
     * Throws JwtException if token is invalid or expired.
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extract roles from JWT token claims.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = validateToken(token);
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }
}
