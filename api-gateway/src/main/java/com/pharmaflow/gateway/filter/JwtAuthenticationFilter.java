package com.pharmaflow.gateway.filter;

import com.pharmaflow.gateway.security.InternalServiceTokenGenerator;
import com.pharmaflow.gateway.security.JwtUtil;
import com.pharmaflow.gateway.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global JWT authentication and authorization filter.
 * - Validates JWT tokens
 * - Enforces role-based access control
 * - Adds internal service token for downstream services
 * - Prevents direct external access to microservices (all must go through gateway)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final InternalServiceTokenGenerator internalTokenGenerator;
    private final TokenBlacklistService tokenBlacklistService;

    // Paths that do NOT require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator" // All actuator endpoints are public for monitoring
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Allow public endpoints without authentication
        if (isPublicPath(path, request.getMethod())) {
            log.debug("Public path accessed: {}", path);
            return addInternalToken(exchange, chain, "anonymous", List.of());
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // Special handling for logout - blacklist the token in Gateway
        if (path.equals("/api/auth/logout")) {
            try {
                Claims claims = jwtUtil.validateToken(token);
                long expirationTime = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (expirationTime > 0) {
                    tokenBlacklistService.blacklistToken(token, expirationTime);
                    log.info("Token blacklisted in Gateway during logout");
                }
            } catch (Exception e) {
                log.warn("Failed to blacklist token during logout: {}", e.getMessage());
            }
            // Continue to backend service
            return addInternalToken(exchange, chain, "user", List.of());
        }

        // Check if token is blacklisted via user-health-service validation
        // (since we don't have distributed Redis, we ask the service that manages blacklist)
        if (isTokenBlacklistedViaService(token)) {
            log.warn("Blacklisted token used for path: {}", path);
            return unauthorizedResponse(exchange, "Token has been revoked");
        }

        try {
            Claims claims = jwtUtil.validateToken(token);
            String username = claims.getSubject();

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            if (roles == null) roles = List.of();

            log.debug("Authenticated user: {} with roles: {} accessing: {}", username, roles, path);

            // Role-based path access control
            if (!hasAccessToPath(path, request.getMethod(), roles)) {
                log.warn("Access denied for user {} with roles {} to path {}", username, roles, path);
                return forbiddenResponse(exchange, "Insufficient permissions");
            }

            // Forward request with user info and internal service token
            return addInternalToken(exchange, chain, username, roles);

        } catch (JwtException e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }
    }

    private Mono<Void> addInternalToken(ServerWebExchange exchange, GatewayFilterChain chain,
                                        String username, List<String> roles) {
        // Generate internal service token
        String internalToken = internalTokenGenerator.generateInternalToken("api-gateway");

        // Add headers for downstream microservices
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Username", username)
                .header("X-Roles", String.join(",", roles))
                .header("X-Internal-Token", internalToken)
                .header("X-Gateway", "pharmaflow-gateway")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Role-based access control for different paths.
     */
    boolean hasAccessToPath(String path, HttpMethod method, List<String> roles) {
        // Admin has access to everything
        if (roles.contains("ROLE_ADMIN")) {
            return true;
        }

        if (path.startsWith("/api/prescriptions")) {
            if (method == HttpMethod.GET || method == HttpMethod.POST) {
                return true;
            }
            return roles.contains("ROLE_DOCTOR") || roles.contains("ROLE_PHARMACIST");
        }

        // Inventory write operations - only pharmacists
        if ((path.startsWith("/api/inventory") || path.startsWith("/api/pharmacies")) &&
            (path.contains("POST") || path.contains("PUT") || path.contains("DELETE")) &&
            !roles.contains("ROLE_PHARMACIST")) {
            return false;
        }

        // User health profiles write - only doctors
        if (path.contains("/patient-profiles") && !roles.contains("ROLE_DOCTOR") && !roles.contains("ROLE_USER")) {
            return false;
        }

        // By default, authenticated users can read
        return true;
    }

    /**
     * Check if token is blacklisted by calling user-health-service.
     * This is needed because without Redis, each service has its own in-memory blacklist.
     */
    private boolean isTokenBlacklistedViaService(String token) {
        // For now, just check local blacklist
        // In production with distributed system, this would call user-health-service
        return tokenBlacklistService.isTokenBlacklisted(token);
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }

        if (method != HttpMethod.GET) {
            return false;
        }

        return isPublicCatalogRead(path);
    }

    boolean isPublicCatalogRead(String path) {
        return path.startsWith("/api/products/")
                || path.equals("/api/products")
                || path.startsWith("/api/categories/")
                || path.equals("/api/categories")
                || path.startsWith("/api/pharmacies/")
                || path.equals("/api/pharmacies")
                || path.startsWith("/api/inventory/product/")
                || path.equals("/api/inventory/product-summary");
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"%s\"}", message);
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }
}
