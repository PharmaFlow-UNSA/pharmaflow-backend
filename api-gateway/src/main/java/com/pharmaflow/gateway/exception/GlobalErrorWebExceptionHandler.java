package com.pharmaflow.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global error handler for Gateway.
 * Provides user-friendly error responses for various failure scenarios.
 */
@Component
@Order(-2) // Higher priority than default error handler
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();

        // Don't intercept actuator endpoints - let Spring handle them
        if (path.startsWith("/actuator")) {
            log.debug("Actuator endpoint error, delegating to default handler: {}", path);
            return Mono.error(ex);
        }

        log.error("Error occurred in Gateway: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        HttpStatus status;

        // Service not found or unavailable (connection refused, timeout, etc.)
        if (ex instanceof NotFoundException ||
            ex.getMessage() != null && (
                ex.getMessage().contains("Connection refused") ||
                ex.getMessage().contains("Service Unavailable") ||
                ex.getMessage().contains("Unable to find instance") ||
                ex.getMessage().contains("No available server")
            )) {

            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorResponse.put("status", 503);
            errorResponse.put("error", "Service Temporarily Unavailable");
            errorResponse.put("message", "The requested service is currently unavailable. Please try again later.");
            errorResponse.put("circuitBreakerActive", true);
            log.warn("Service unavailable: {}", ex.getMessage());

        } else {
            // Generic error
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorResponse.put("status", 500);
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "An unexpected error occurred. Please contact support if the problem persists.");
            log.error("Unexpected error in Gateway", ex);
        }

        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("path", exchange.getRequest().getURI().getPath());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            String fallback = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Error processing error response\"}";
            byte[] bytes = fallback.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}

