package com.pharmaflow.userhealth.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate with client-side load balancing.
 * The @LoadBalanced annotation enables Ribbon/Spring Cloud LoadBalancer
 * for inter-service communication through Eureka service discovery.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a load-balanced RestTemplate bean for making HTTP requests
     * to other microservices using their service names registered in Eureka.
     *
     * Example usage:
     * restTemplate.getForObject("http://ORDER-SERVICE/api/orders/1", OrderDTO.class);
     *
     * @return RestTemplate with load balancing enabled
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

