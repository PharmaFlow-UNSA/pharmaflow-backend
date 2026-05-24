package com.pharmaflow.userhealth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.secret:pharmaflow-secret-key-2024-very-long-and-secure-key-for-production}")
    private String secret;

    @Value("${jwt.expiration:3600000}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generate JWT token for authenticated user.
     * Token contains username (email) and roles.
     * Expiration is 1 hour by default (configurable).
     */
    public String generateToken(String email, List<String> roles) {
        return generateToken(email, roles, null, null, null);
    }

    public String generateToken(String email, List<String> roles, Long userId) {
        return generateToken(email, roles, userId, null, null);
    }

    public String generateToken(String email, List<String> roles, Long userId,
                                String firstName, String lastName) {
        var builder = Jwts.builder()
                .subject(email)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration));
        if (userId != null)    { builder.claim("userId", userId);       }
        if (firstName != null) { builder.claim("firstName", firstName); }
        if (lastName  != null) { builder.claim("lastName",  lastName);  }
        return builder.signWith(getSigningKey()).compact();
    }

    /**
     * Validate token and return claims.
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

    public Long extractUserId(String token) {
        Claims claims = validateToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Extract roles from JWT token.
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

    /**
     * Get remaining time until token expiration (in milliseconds).
     * Used for blacklisting tokens.
     */
    public long getExpirationTime(String token) {
        Claims claims = validateToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        return Math.max(0, expiration.getTime() - now.getTime());
    }

    /**
     * Get configured token expiration duration (in milliseconds).
     * Used in auth responses to inform client when token expires.
     */
    public long getExpirationDuration() {
        return expiration;
    }
}
