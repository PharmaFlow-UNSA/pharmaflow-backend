package com.pharmaflow.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for circuit breaker.
 * Provides graceful degradation when downstream services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/user-health")
    public ResponseEntity<Map<String, Object>> userHealthFallback() {
        log.warn("User Health Service is temporarily unavailable - circuit breaker activated");
        return createFallbackResponse("User Health Service");
    }

    @GetMapping("/product-health")
    public ResponseEntity<Map<String, Object>> productHealthFallback() {
        log.warn("Product Health Service is temporarily unavailable - circuit breaker activated");
        return createFallbackResponse("Product Health Service");
    }

    @GetMapping("/order-prescription")
    public ResponseEntity<Map<String, Object>> orderPrescriptionFallback() {
        log.warn("Order Prescription Service is temporarily unavailable - circuit breaker activated");
        return createFallbackResponse("Order Prescription Service");
    }

    @GetMapping("/pharmacy-inventory")
    public ResponseEntity<Map<String, Object>> pharmacyInventoryFallback() {
        log.warn("Pharmacy Inventory Service is temporarily unavailable - circuit breaker activated");
        return createFallbackResponse("Pharmacy Inventory Service");
    }

    @GetMapping("/smart-features")
    public ResponseEntity<Map<String, Object>> smartFeaturesFallback() {
        log.warn("Smart Features Service is temporarily unavailable - circuit breaker activated");
        return createFallbackResponse("Smart Features Service");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Temporarily Unavailable");
        response.put("message", serviceName + " is currently unavailable. Please try again later.");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("circuitBreakerActive", true);

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}

