package com.pharmaflow.producthealth.config;

import com.pharmaflow.producthealth.security.JwtAuthenticationFilter;
import com.pharmaflow.producthealth.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

/**
 * Security configuration for Product & Medical Service.
 *
 * Authorization rules:
 * - GET /api/products, /api/categories, /api/substances, /api/substitutes
 *     → any authenticated user (ROLE_USER and above)
 * - GET /api/interactions, /api/contraindications
 *     → ROLE_DOCTOR, ROLE_PHARMACIST, ROLE_ADMIN (medical roles)
 * - POST, PUT, PATCH on all endpoints
 *     → ROLE_PHARMACIST, ROLE_ADMIN
 * - DELETE on all endpoints
 *     → ROLE_ADMIN only
 *
 * Authentication is NOT handled here.
 * Login/register is in user-health-service (/api/auth/login).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil) {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public infrastructure
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(
                                "/swagger", "/swagger/**", "/swagger-ui.html",
                                "/swagger-ui/**", "/api-docs", "/api-docs/**",
                                "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/load-balance-test/**",
                                "/api/load-balancer-demo/**").permitAll()

                        // GET - products, categories, substances, substitutes
                        // any authenticated user can read
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/substances/**",
                                "/api/substitutes/**").authenticated()

                        // GET - drug interactions and contraindications
                        // only medical staff
                        .requestMatchers(HttpMethod.GET,
                                "/api/interactions/**",
                                "/api/contraindications/**")
                                .hasAnyRole("DOCTOR", "PHARMACIST", "ADMIN")

                        // POST, PUT, PATCH - pharmacist or admin
                        .requestMatchers(HttpMethod.POST, "/**")
                                .hasAnyRole("PHARMACIST", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/**")
                                .hasAnyRole("PHARMACIST", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/**")
                                .hasAnyRole("PHARMACIST", "ADMIN")

                        // DELETE - admin only
                        .requestMatchers(HttpMethod.DELETE, "/**")
                                .hasRole("ADMIN")

                        // Everything else - authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\"," +
                                    "\"message\":\"Authentication required. Please login via /api/auth/login on user-health-service.\"," +
                                    "\"path\":\"%s\"}",
                                    Instant.now(), request.getRequestURI()));
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\"," +
                                    "\"message\":\"Insufficient permissions. Required role not granted.\"," +
                                    "\"path\":\"%s\"}",
                                    Instant.now(), request.getRequestURI()));
                        })
                );

        return http.build();
    }
}
