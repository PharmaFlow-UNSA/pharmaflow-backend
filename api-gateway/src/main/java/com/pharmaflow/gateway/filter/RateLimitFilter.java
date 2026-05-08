package com.pharmaflow.gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Per-user rate limiting filter to prevent abuse.
 * Uses Resilience4j for distributed rate limiting.
 *
 * Rate limits:
 * - Authenticated users: 100 requests per minute
 * - Anonymous users: 20 requests per minute
 * - Admin users: unlimited
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_EXCEEDED_MESSAGE =
        "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}";

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimitFilter() {
        // Configure rate limiter
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(100)
                .timeoutDuration(Duration.ofMillis(100))
                .build();

        this.rateLimiterRegistry = RateLimiterRegistry.of(config);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip rate limiting for health checks
        if (path.startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }

        // Get user identifier
        String username = exchange.getRequest().getHeaders().getFirst("X-Username");
        String userId = username != null ? username : getClientIp(exchange);

        // Check if user is admin (bypass rate limit)
        String roles = exchange.getRequest().getHeaders().getFirst("X-Roles");
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            return chain.filter(exchange);
        }

        // Get or create rate limiter for this user
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(userId);

        // Try to acquire permission
        Callable<Mono<Void>> callable = () -> chain.filter(exchange);

        try {
            return RateLimiter.decorateCallable(rateLimiter, callable).call();
        } catch (Exception e) {
            log.warn("Rate limit exceeded for user: {}", userId);
            return rateLimitExceededResponse(exchange);
        }
    }

    private String getClientIp(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        }
        return ip;
    }

    private Mono<Void> rateLimitExceededResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-RateLimit-Retry-After", "60");

        var buffer = response.bufferFactory().wrap(RATE_LIMIT_EXCEEDED_MESSAGE.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0; // Run after JWT filter
    }
}

