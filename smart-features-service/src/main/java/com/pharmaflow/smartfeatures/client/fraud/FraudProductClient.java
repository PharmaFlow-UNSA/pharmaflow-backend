package com.pharmaflow.smartfeatures.client.fraud;

import com.pharmaflow.smartfeatures.client.ExternalServiceRestClientFactory;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalProductDto;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FraudProductClient {

  private final RestClient restClient;

  public FraudProductClient(
      @Value("${smartfeatures.product-service.service-id:product-health-service}") String serviceId,
      ExternalServiceRestClientFactory restClientFactory) {
    this.restClient = restClientFactory.create(serviceId);
  }

  public Optional<ExternalProductDto> getProduct(Long productId) {
    try {
      return Optional.ofNullable(
          restClient
              .get()
              .uri("/api/products/{id}", productId)
              .retrieve()
              .body(ExternalProductDto.class));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw unavailable(ex);
    } catch (RestClientException ex) {
      throw unavailable(ex);
    }
  }

  private ExternalServiceException unavailable(Exception ex) {
    return new ExternalServiceException(
        "product-health-service is unavailable: " + ex.getMessage());
  }
}
