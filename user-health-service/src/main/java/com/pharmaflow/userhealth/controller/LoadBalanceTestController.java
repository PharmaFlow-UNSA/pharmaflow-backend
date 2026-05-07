package com.pharmaflow.userhealth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for testing load balancing across multiple instances.
 * Returns information about which instance handled the request.
 */
@RestController
@RequestMapping("/api/load-balance-test")
@Tag(name = "Load Balancing Test", description = "Endpoint for testing load balancing distribution")
public class LoadBalanceTestController {

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${eureka.instance.instance-id:unknown}")
    private String instanceId;

    /**
     * Returns information about the current instance handling the request.
     * Used by load-balance-test.ps1 script to track request distribution.
     *
     * @return Map containing instance information
     */
    @GetMapping
    @Operation(summary = "Get instance info", description = "Returns information about which instance is handling the request")
    public ResponseEntity<Map<String, String>> getInstanceInfo() {
        Map<String, String> instanceInfo = new HashMap<>();

        instanceInfo.put("applicationName", applicationName);
        instanceInfo.put("instanceId", instanceId);
        instanceInfo.put("serverPort", serverPort);
        instanceInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));

        try {
            instanceInfo.put("hostname", InetAddress.getLocalHost().getHostName());
            instanceInfo.put("hostAddress", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            instanceInfo.put("hostname", "unknown");
            instanceInfo.put("hostAddress", "unknown");
        }

        return ResponseEntity.ok(instanceInfo);
    }
}

