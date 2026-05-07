package com.pharmaflow.smartfeatures.client.product;

import com.pharmaflow.smartfeatures.client.ExternalServiceRestClientFactory;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.ProductSubstituteSnapshot;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductCatalogClient {

  private final RestClient restClient;

  public ProductCatalogClient(
      @Value("${smartfeatures.product-service.service-id:product-health-service}") String serviceId,
      ExternalServiceRestClientFactory restClientFactory) {
    this.restClient = restClientFactory.create(serviceId);
  }

  public List<ProductSnapshot> getActiveProducts() {
    try {
      ProductSnapshot[] products =
          restClient.get().uri("/api/products").retrieve().body(ProductSnapshot[].class);
      return products == null ? List.of() : Arrays.asList(products);
    } catch (RestClientException ex) {
      throw unavailable(ex);
    }
  }

  public Optional<ProductSnapshot> getProduct(Long productId) {
    try {
      return Optional.ofNullable(
          restClient
              .get()
              .uri("/api/products/{id}", productId)
              .retrieve()
              .body(ProductSnapshot.class));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw unavailable(ex);
    } catch (RestClientException ex) {
      throw unavailable(ex);
    }
  }

  public List<ProductSubstituteSnapshot> getSubstitutesForProduct(Long productId) {
    try {
      ProductSubstituteSnapshot[] substitutes =
          restClient
              .get()
              .uri("/api/substitutes/product/{productId}", productId)
              .retrieve()
              .body(ProductSubstituteSnapshot[].class);
      return substitutes == null ? List.of() : Arrays.asList(substitutes);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return List.of();
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
