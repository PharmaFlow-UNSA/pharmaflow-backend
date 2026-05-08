package com.pharmaflow.smartfeatures.client.fraud;

import com.pharmaflow.smartfeatures.client.ExternalServiceRestClientFactory;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalOrderDto;
import com.pharmaflow.smartfeatures.dto.fraud.external.ExternalPrescriptionDto;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import com.pharmaflow.smartfeatures.exception.ResourceNotFoundException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FraudOrderClient {

  private final RestClient restClient;

  public FraudOrderClient(
      @Value("${smartfeatures.order-service.service-id:order-prescription-service}")
          String serviceId,
      ExternalServiceRestClientFactory restClientFactory) {
    this.restClient = restClientFactory.create(serviceId);
  }

  public ExternalOrderDto getOrder(Long orderId) {
    return getRequired(
        "/api/orders/{id}", ExternalOrderDto.class, "Order not found with id: " + orderId, orderId);
  }

  public List<ExternalOrderDto> getOrdersByUser(Long userId) {
    return getList("/api/orders/user/{userId}", ExternalOrderDto[].class, userId);
  }

  public List<ExternalOrderDto> getAllOrders() {
    return getList("/api/orders", ExternalOrderDto[].class);
  }

  public List<ExternalPrescriptionDto> getPrescriptionsByUser(Long userId) {
    return getList("/api/prescriptions/user/{userId}", ExternalPrescriptionDto[].class, userId);
  }

  public List<ExternalPrescriptionDto> getAllPrescriptions() {
    return getList("/api/prescriptions", ExternalPrescriptionDto[].class);
  }

  private <T> T getRequired(
      String uri, Class<T> responseType, String notFoundMessage, Object... uriVariables) {
    try {
      return restClient.get().uri(uri, uriVariables).retrieve().body(responseType);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        throw new ResourceNotFoundException(notFoundMessage);
      }
      throw unavailable("order-prescription-service", ex);
    } catch (RestClientException ex) {
      throw unavailable("order-prescription-service", ex);
    }
  }

  private <T> List<T> getList(String uri, Class<T[]> responseType, Object... uriVariables) {
    try {
      T[] response = restClient.get().uri(uri, uriVariables).retrieve().body(responseType);
      return response == null ? List.of() : Arrays.asList(response);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return List.of();
      }
      throw unavailable("order-prescription-service", ex);
    } catch (RestClientException ex) {
      throw unavailable("order-prescription-service", ex);
    }
  }

  private ExternalServiceException unavailable(String serviceName, Exception ex) {
    return new ExternalServiceException(serviceName + " is unavailable: " + ex.getMessage());
  }
}
