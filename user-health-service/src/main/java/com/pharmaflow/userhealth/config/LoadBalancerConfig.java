package com.pharmaflow.userhealth.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for load-balanced RestTemplate.
 * This enables client-side load balancing using Spring Cloud LoadBalancer.
 */
@Configuration
public class LoadBalancerConfig {

    /**
     * Creates a load-balanced RestTemplate bean.
     * When calling services by name (e.g., http://USER-HEALTH-SERVICE/api/...),
     * Spring Cloud LoadBalancer will automatically select an available instance from Eureka.
     *
     * @return Load-balanced RestTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}

