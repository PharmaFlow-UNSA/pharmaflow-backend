package com.pharmaflow.pharmacyinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PharmacyInventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PharmacyInventoryServiceApplication.class, args);
    }

}
