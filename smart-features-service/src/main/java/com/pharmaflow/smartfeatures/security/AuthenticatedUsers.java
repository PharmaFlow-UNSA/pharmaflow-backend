package com.pharmaflow.smartfeatures.security;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class AuthenticatedUsers {

  private AuthenticatedUsers() {}

  public static AuthenticatedUser from(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      return user;
    }
    String subject = authentication != null ? authentication.getName() : null;
    return new AuthenticatedUser(subject, parseUserId(subject));
  }

  public static boolean isAdmin(Authentication authentication) {
    if (authentication == null) {
      return false;
    }
    Set<String> authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    return authorities.contains("ROLE_ADMIN");
  }

  private static Long parseUserId(String subject) {
    if (subject == null) {
      return null;
    }
    try {
      return Long.valueOf(subject);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
