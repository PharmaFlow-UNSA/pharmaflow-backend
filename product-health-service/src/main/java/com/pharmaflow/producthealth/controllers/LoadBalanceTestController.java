package com.pharmaflow.producthealth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller that returns information about the instance that handled the request.
 * Used for demonstrating load balancing — each of the requests
 * can be handled by different instances.
 */
@RestController
@RequestMapping("/api/load-balance-test")
@Tag(name = "Load Balancer Test", description = "Endpoint for load balancing testing")
public class LoadBalanceTestController {

    @Value("${server.port:8082}")
    private String serverPort;

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping
    @Operation(
            summary = "Get instance information",
            description = "Each request returns the port and hostname of the instance that handled it. " +
                    "Useful for verifying load balancing through Eureka."
    )
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", applicationName);
        info.put("port", serverPort);
        info.put("instanceId", applicationName + ":" + serverPort);
        try {
            info.put("hostname", InetAddress.getLocalHost().getHostName());
            info.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            info.put("hostname", "unknown");
        }
        info.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(info);
    }
}