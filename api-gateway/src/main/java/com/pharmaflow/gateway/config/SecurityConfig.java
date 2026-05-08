package com.pharmaflow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for the API Gateway.
 *
 * Authentication is handled entirely by the JwtAuthenticationFilter (GlobalFilter).
 * Spring Security is configured to permit all requests at the framework level
 * so that our custom JWT filter has full control over access decisions.
 *
 * This avoids conflicts between Spring Security's built-in auth and our JWT logic.
 *
 * Following Spring Security 6 best practices:
 * - Disable CSRF (not needed for stateless JWT auth)
 * - Disable HTTP Basic (using JWT instead)
 * - Disable form login (API only)
 * - Custom JWT filter handles all authentication
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF for stateless JWT authentication
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Allow all requests - JWT filter handles authorization
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )

                // Disable HTTP Basic authentication
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // Disable form login (API only)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Disable logout (handled by user-health-service)
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                .build();
    }
}
