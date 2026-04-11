package com.pharmaflow.userhealth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userHealthServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User & Health Microservice API")
                        .description("Production-ready REST API for managing users, family members, and health profiles in PharmaFlow system")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PharmaFlow Team")
                                .email("support@pharmaflow.ba")));
    }
}

