package com.pharmaflow.producthealth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productHealthServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product & Medical Microservice API")
                        .description("REST API for managing pharmaceutical products, substances, drug interactions, contraindications and substitutions — PharmaFlow system")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PharmaFlow Team")
                                .email("support@pharmaflow.ba")));
    }
}
