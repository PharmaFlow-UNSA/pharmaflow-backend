package com.pharmaflow.orderprescription.controller;

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
 * Uses @LoadBalanced RestTemplate to call the service through Eureka,
 * demonstrating client-side load balancing in action.
 */
@RestController
@RequestMapping("/api/load-balancer-demo")
@Tag(name = "Load Balancer Demo", description = "Demonstrates load balancing through Eureka")
public class LoadBalancerDemoController {

    @Autowired
    private RestTemplate loadBalancedRestTemplate;

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping
    @Operation(
            summary = "Call service through load balancer",
            description = "Calls the load-balance-test endpoint through Eureka, demonstrating load balancing"
    )
    public ResponseEntity<Map> callThroughLoadBalancer() {
        String serviceUrl = "http://" + applicationName.toUpperCase() + "/api/load-balance-test";
        Map response = loadBalancedRestTemplate.getForObject(serviceUrl, Map.class);
        return ResponseEntity.ok(response);
    }
}
