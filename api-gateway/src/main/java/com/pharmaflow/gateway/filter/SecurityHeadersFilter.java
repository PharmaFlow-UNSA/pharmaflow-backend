package com.pharmaflow.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security headers filter - adds security-related HTTP headers.
 * Implements OWASP best practices for 2024/2026.
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Prevent clickjacking attacks
            headers.add("X-Frame-Options", "DENY");

            // Prevent MIME-type sniffing
            headers.add("X-Content-Type-Options", "nosniff");

            // Enable XSS protection
            headers.add("X-XSS-Protection", "1; mode=block");

            // Strict Transport Security (HTTPS only in production)
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

            // Content Security Policy
            headers.add("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");

            // Referrer Policy
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions Policy
            headers.add("Permissions-Policy",
                "geolocation=(), microphone=(), camera=()");
        }));
    }

    @Override
    public int getOrder() {
        return -2; // Run before JWT filter
    }
}

