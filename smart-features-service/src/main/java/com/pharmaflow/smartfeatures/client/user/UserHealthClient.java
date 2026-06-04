package com.pharmaflow.smartfeatures.client.user;

import com.pharmaflow.smartfeatures.client.ExternalServiceRestClientFactory;
import com.pharmaflow.smartfeatures.dto.userhealth.FamilyMemberSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.PatientHealthProfileSnapshot;
import com.pharmaflow.smartfeatures.dto.userhealth.UserHealthSnapshot;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class UserHealthClient {

  private final RestClient restClient;

  public UserHealthClient(
      @Value("${smartfeatures.user-service.service-id:user-health-service}") String serviceId,
      ExternalServiceRestClientFactory restClientFactory) {
    this.restClient = restClientFactory.create(serviceId);
  }

  public Optional<UserHealthSnapshot> getUser(Long userId) {
    try {
      return Optional.ofNullable(
          restClient
              .get()
              .uri("/api/users/{id}", userId)
              .retrieve()
              .body(UserHealthSnapshot.class));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw unavailable(ex);
    } catch (RestClientException ex) {
      throw unavailable(ex);
    }
  }

  public Optional<PatientHealthProfileSnapshot> getPatientProfile(Long patientProfileId) {
    try {
      return Optional.ofNullable(
          restClient
              .get()
              .uri("/api/patient-profiles/{id}", patientProfileId)
              .retrieve()
              .body(PatientHealthProfileSnapshot.class));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw unavailable(ex);
    } catch (RestClientException ex) {
      throw unavailable(ex);
    }
  }

  public List<FamilyMemberSnapshot> getFamilyMembersByUserId(Long userId) {
    try {
      FamilyMemberSnapshot[] members =
          restClient
              .get()
              .uri("/api/family-members/user/{userId}", userId)
              .retrieve()
              .body(FamilyMemberSnapshot[].class);
      return members == null ? List.of() : Arrays.asList(members);
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
    return new ExternalServiceException("user-health-service is unavailable: " + ex.getMessage());
  }
}
