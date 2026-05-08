package com.pharmaflow.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global error attributes for consistent error responses across the gateway.
 * Provides structured error information for debugging and client consumption.
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorAttributes.class);

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);

        Throwable error = getError(request);

        // Add custom attributes
        errorAttributes.put("timestamp", LocalDateTime.now().toString());
        errorAttributes.put("path", request.path());
        errorAttributes.put("method", request.method().name());

        // Log error for monitoring
        log.error("Gateway error on path {}: {}",
            request.path(),
            error != null ? error.getMessage() : "Unknown error");

        return errorAttributes;
    }
}

