package com.pharmaflow.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Programmatic gateway route configuration.
 * Routes are also defined in application.properties —
 * this class adds additional filters for resilience and fault tolerance:
 * - Retry: Automatic retry on transient failures
 * - Custom Headers: Gateway identification
 *
 * Note: Circuit Breaker configuration is handled via application.properties
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // User & Health Service
                .route("user-health-service", r -> r
                        .path("/api/users/**", "/api/auth/**",
                              "/api/family-members/**", "/api/allergies/**",
                              "/api/therapies/**", "/api/patient-profiles/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "pharmaflow-gateway")
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(org.springframework.http.HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri("lb://USER-HEALTH-SERVICE"))

                // Product & Medical Service
                .route("product-health-service", r -> r
                        .path("/api/products/**", "/api/categories/**",
                              "/api/substances/**", "/api/interactions/**",
                              "/api/contraindications/**", "/api/substitutes/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "pharmaflow-gateway")
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(org.springframework.http.HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri("lb://PRODUCT-HEALTH-SERVICE"))

                // Order & Prescription Service
                .route("order-prescription-service", r -> r
                        .path("/api/orders/**", "/api/prescriptions/**", "/api/payments/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "pharmaflow-gateway")
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(org.springframework.http.HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri("lb://ORDER-PRESCRIPTION-SERVICE"))

                // Pharmacy & Inventory Service
                .route("pharmacy-inventory-service", r -> r
                        .path("/api/pharmacies/**", "/api/inventory/**", "/api/reservations/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "pharmaflow-gateway")
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(org.springframework.http.HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri("lb://PHARMACY-INVENTORY-SERVICE"))

                // Smart Features Service
                .route("smart-features-service", r -> r
                        .path("/api/symptoms/**", "/api/symptom-searches/**", "/api/recommendations/**",
                              "/api/notifications/**", "/api/fraud/**",
                              "/api/fraud-rules/**", "/api/fraud-checks/**",
                              "/api/chatbot/**", "/api/faqs/**", "/api/admin/faqs/**",
                              "/api/chat-sessions/**", "/api/chat-messages/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway", "pharmaflow-gateway")
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(org.springframework.http.HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri("lb://SMART-FEATURES-SERVICE"))

                .build();
    }
}
