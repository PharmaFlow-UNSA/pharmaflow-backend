package com.pharmaflow.userhealth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Controller for demonstrating load balancing.
 * This endpoint uses @LoadBalanced RestTemplate to call the service through Eureka,
 * which demonstrates client-side load balancing in action.
 */
@RestController
@RequestMapping("/api/load-balancer-demo")
@Tag(name = "Load Balancer Demo", description = "Demonstrates load balancing through Eureka")
public class LoadBalancerDemoController {

    @Autowired
    private RestTemplate loadBalancedRestTemplate;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * This endpoint calls the /api/load-balance-test endpoint through Eureka.
     * Each request will be load-balanced across all available instances.
     *
     * @return Instance information from the instance that handled the request
     */
    @GetMapping
    @Operation(
            summary = "Call service through load balancer",
            description = "Calls the load-balance-test endpoint through Eureka, demonstrating load balancing"
    )
    public ResponseEntity<Map> callThroughLoadBalancer() {
        // Call the service by name (not by host:port)
        // Spring Cloud LoadBalancer will automatically select an available instance
        String serviceUrl = "http://" + applicationName.toUpperCase() + "/api/load-balance-test";

        Map response = loadBalancedRestTemplate.getForObject(serviceUrl, Map.class);
        return ResponseEntity.ok(response);
    }
}

