package com.pharmaflow.orderprescription.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderPrescriptionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order & Prescription Microservice API")
                        .description("Production-ready REST API for managing orders, prescriptions, payments and auto-refill subscriptions in PharmaFlow system")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PharmaFlow Team")
                                .email("support@pharmaflow.ba")));
    }
}
