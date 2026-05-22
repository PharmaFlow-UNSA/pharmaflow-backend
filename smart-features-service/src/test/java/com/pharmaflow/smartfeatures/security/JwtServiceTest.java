package com.pharmaflow.smartfeatures.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET =
      "pharmaflow-secret-key-2024-very-long-and-secure-key-for-production";

  private final JwtService jwtService = new JwtService(SECRET);

  @Test
  void validTokenReturnsSubjectAndRoles() {
    String token = token("user@example.com", List.of("ROLE_USER"), Instant.now().plusSeconds(3600));

    assertThat(jwtService.extractSubject(token)).isEqualTo("user@example.com");
    assertThat(jwtService.extractRoles(token)).containsExactly("ROLE_USER");
  }

  @Test
  void expiredTokenFailsValidation() {
    String token = token("user@example.com", List.of("ROLE_USER"), Instant.now().minusSeconds(60));

    assertThatThrownBy(() -> jwtService.parseAndValidate(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void tamperedTokenFailsValidation() {
    String token = token("user@example.com", List.of("ROLE_USER"), Instant.now().plusSeconds(3600));
    String tamperedToken = token.substring(0, token.length() - 2) + "xx";

    assertThatThrownBy(() -> jwtService.parseAndValidate(tamperedToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void tokenWithoutRolesClaimReturnsEmptyRoles() {
    String token =
        Jwts.builder()
            .subject("user@example.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(signingKey())
            .compact();

    assertThat(jwtService.extractRoles(token)).isEmpty();
  }

  private String token(String subject, List<String> roles, Instant expiration) {
    return Jwts.builder()
        .subject(subject)
        .claim("roles", roles)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(expiration))
        .signWith(signingKey())
        .compact();
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }
}
