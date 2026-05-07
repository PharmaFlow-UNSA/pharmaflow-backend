package com.pharmaflow.smartfeatures.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI smartFeaturesOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("PharmaFlow Smart Features API")
                .description("API documentation for the smart-features-service module.")
                .version("v1"));
  }
}
