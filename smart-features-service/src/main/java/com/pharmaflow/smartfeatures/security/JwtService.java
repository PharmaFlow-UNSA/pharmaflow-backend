package com.pharmaflow.smartfeatures.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecretKey signingKey;

  public JwtService(
      @Value("${jwt.secret:pharmaflow-secret-key-2024-very-long-and-secure-key-for-production}")
          String secret) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public Claims parseAndValidate(String token) throws JwtException {
    Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    if (claims.getSubject() == null || claims.getSubject().isBlank()) {
      throw new MalformedJwtException("JWT subject is required");
    }
    return claims;
  }

  public String extractSubject(String token) throws JwtException {
    return parseAndValidate(token).getSubject();
  }

  @SuppressWarnings("unchecked")
  public List<String> extractRoles(Claims claims) {
    Object roles = claims.get("roles");
    if (roles instanceof List<?> list) {
      return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }
    return Collections.emptyList();
  }

  public List<String> extractRoles(String token) throws JwtException {
    return extractRoles(parseAndValidate(token));
  }
}
