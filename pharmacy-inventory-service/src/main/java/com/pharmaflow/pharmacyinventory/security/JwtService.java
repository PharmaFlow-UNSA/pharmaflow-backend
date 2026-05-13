package com.pharmaflow.pharmacyinventory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

@Component
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret:pharmaflow-secret-key-2024-very-long-and-secure-key-for-production}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) throws JwtException {
        return parseAndValidate(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) throws JwtException {
        Object roles = parseAndValidate(token).get("roles");
        if (roles instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }
}
