package com.pharmaflow.producthealth.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Konfiguracija za load-balanced RestTemplate.
 * Enables client-side load balancing through Spring Cloud LoadBalancer.
 * Pozivi prema drugim mikroservisima koriste naziv servisa iz Eureke
 * (npr. http://USER-HEALTH-SERVICE/api/...) umjesto hardkodiranog host:port.
 */
@Configuration
public class LoadBalancerConfig {

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}
