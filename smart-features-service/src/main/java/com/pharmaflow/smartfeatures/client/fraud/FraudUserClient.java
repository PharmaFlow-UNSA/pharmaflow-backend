package com.pharmaflow.smartfeatures.client.fraud;

import com.pharmaflow.smartfeatures.client.ExternalServiceRestClientFactory;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalUserDto;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FraudUserClient {

  private final RestClient restClient;

  public FraudUserClient(
      @Value("${smartfeatures.user-service.service-id:user-health-service}") String serviceId,
      ExternalServiceRestClientFactory restClientFactory) {
    this.restClient = restClientFactory.create(serviceId);
  }

  public Optional<ExternalUserDto> getUser(Long userId) {
    try {
      return Optional.ofNullable(
          restClient.get().uri("/api/users/{id}", userId).retrieve().body(ExternalUserDto.class));
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
    return new ExternalServiceException("user-health-service is unavailable: " + ex.getMessage());
  }
}
