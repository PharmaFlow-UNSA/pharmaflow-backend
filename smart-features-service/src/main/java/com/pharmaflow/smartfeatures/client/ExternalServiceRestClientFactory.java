package com.pharmaflow.smartfeatures.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class ExternalServiceRestClientFactory {

  private final RestClient.Builder loadBalancedRestClientBuilder;
  private final AuthorizationHeaderForwardingInterceptor authorizationHeaderForwardingInterceptor;

  public ExternalServiceRestClientFactory(
      @Qualifier("loadBalancedRestClientBuilder")
          RestClient.Builder loadBalancedRestClientBuilder,
      AuthorizationHeaderForwardingInterceptor authorizationHeaderForwardingInterceptor) {
    this.loadBalancedRestClientBuilder = loadBalancedRestClientBuilder;
    this.authorizationHeaderForwardingInterceptor = authorizationHeaderForwardingInterceptor;
  }

  public RestClient create(String serviceId) {
    if (!StringUtils.hasText(serviceId)) {
      throw new IllegalArgumentException("External service id must be configured.");
    }

    return loadBalancedRestClientBuilder
        .clone()
        .requestInterceptor(authorizationHeaderForwardingInterceptor)
        .baseUrl("http://" + serviceId.trim())
        .build();
  }
}
