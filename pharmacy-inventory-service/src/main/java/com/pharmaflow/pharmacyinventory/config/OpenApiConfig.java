package com.pharmaflow.pharmacyinventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pharmacyInventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pharmacy & Inventory Microservice API")
                        .description("Production-ready REST API for managing pharmacies, inventory, reservations and deliveries in PharmaFlow system")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PharmaFlow Team")
                                .email("support@pharmaflow.ba")));
    }
}
