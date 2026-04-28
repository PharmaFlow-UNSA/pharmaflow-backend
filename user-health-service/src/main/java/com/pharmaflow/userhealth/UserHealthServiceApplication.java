package com.pharmaflow.userhealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserHealthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserHealthServiceApplication.class, args);
    }

}