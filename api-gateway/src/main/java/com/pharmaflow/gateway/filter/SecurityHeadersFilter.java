package com.pharmaflow.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security headers filter - adds OWASP-recommended security headers.
 *
 * IMPORTANT: must register the header writes through {@code beforeCommit(...)}.
 * Spring Cloud Gateway streams the upstream response straight through Netty,
 * so by the time {@code chain.filter(exchange).then(...)} fires, the response
 * is already committed and any mutation throws UnsupportedOperationException,
 * which terminates the chunked stream mid-flight (ERR_INCOMPLETE_CHUNKED_ENCODING
 * in the browser, "transfer closed with outstanding read data remaining" in curl).
 *
 * Using {@code beforeCommit} runs the callback right before the response is
 * flushed, after the upstream proxy populated its headers but before they
 * cross the wire.
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Spring Security's default writers may already set some of these;
            // only add when missing so we don't duplicate header values.
            addIfAbsent(headers, "X-Frame-Options", "DENY");
            addIfAbsent(headers, "X-Content-Type-Options", "nosniff");
            addIfAbsent(headers, "X-XSS-Protection", "1; mode=block");
            addIfAbsent(headers, "Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains");
            addIfAbsent(headers, "Content-Security-Policy",
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; "
                            + "style-src 'self' 'unsafe-inline'");
            addIfAbsent(headers, "Referrer-Policy", "strict-origin-when-cross-origin");
            addIfAbsent(headers, "Permissions-Policy",
                    "geolocation=(), microphone=(), camera=()");

            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    private static void addIfAbsent(HttpHeaders headers, String name, String value) {
        if (!headers.containsKey(name)) {
            headers.add(name, value);
        }
    }

    @Override
    public int getOrder() {
        return -2; // Run before JWT filter
    }
}
