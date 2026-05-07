package com.pharmaflow.smartfeatures.service.discovery;

import com.pharmaflow.smartfeatures.dto.discovery.DiscoveryProbeResponseDto;
import com.pharmaflow.smartfeatures.exception.BadRequestException;
import com.pharmaflow.smartfeatures.exception.ExternalServiceException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DiscoveryProbeService {

  private static final String HEALTH_PATH = "/actuator/health";

  private final LoadBalancerClient loadBalancerClient;
  private final DiscoveryClient discoveryClient;
  private final RestClient restClient;
  private final Map<String, TargetService> targetServices;

  public DiscoveryProbeService(
      LoadBalancerClient loadBalancerClient,
      DiscoveryClient discoveryClient,
      @Qualifier("directRestClientBuilder") RestClient.Builder restClientBuilder,
      @Value("${smartfeatures.product-service.service-id:product-health-service}")
          String productServiceId,
      @Value("${smartfeatures.order-service.service-id:order-prescription-service}")
          String orderServiceId,
      @Value("${smartfeatures.user-service.service-id:user-health-service}") String userServiceId) {
    this.loadBalancerClient = loadBalancerClient;
    this.discoveryClient = discoveryClient;
    this.restClient = restClientBuilder.clone().build();
    this.targetServices =
        Map.of(
            "product", new TargetService("product", productServiceId),
            "order", new TargetService("order", orderServiceId),
            "user", new TargetService("user", userServiceId));
  }

  public DiscoveryProbeResponseDto probe(String serviceKey, String mode) {
    if ("direct".equalsIgnoreCase(mode)) {
      return probeDirect(serviceKey);
    }
    if ("load-balanced".equalsIgnoreCase(mode) || "load_balanced".equalsIgnoreCase(mode)) {
      return probeLoadBalanced(serviceKey);
    }
    throw new BadRequestException("Unsupported discovery probe mode: " + mode);
  }

  private DiscoveryProbeResponseDto probeDirect(String serviceKey) {
    TargetService targetService = resolveTargetService(serviceKey);
    List<ServiceInstance> instances = discoveryClient.getInstances(targetService.serviceId());

    if (instances.isEmpty()) {
      throw new ExternalServiceException(
          "No Eureka instances available for " + targetService.serviceId());
    }

    ServiceInstance instance =
        instances.stream()
            .sorted(Comparator.comparing(this::stableInstanceKey))
            .findFirst()
            .orElseThrow();
    URI healthUri =
        UriComponentsBuilder.fromUri(instance.getUri()).path(HEALTH_PATH).build().toUri();
    return callHealthEndpoint(
        "direct", targetService, instance.getInstanceId(), instance.getUri().toString(), healthUri);
  }

  private DiscoveryProbeResponseDto probeLoadBalanced(String serviceKey) {
    TargetService targetService = resolveTargetService(serviceKey);
    ServiceInstance instance = loadBalancerClient.choose(targetService.serviceId());

    if (instance == null) {
      throw new ExternalServiceException(
          "No Eureka instances available for " + targetService.serviceId());
    }

    URI healthUri =
        UriComponentsBuilder.fromUri(instance.getUri()).path(HEALTH_PATH).build().toUri();
    return callHealthEndpoint(
        "load-balanced",
        targetService,
        instance.getInstanceId(),
        instance.getUri().toString(),
        healthUri);
  }

  private DiscoveryProbeResponseDto callHealthEndpoint(
      String mode,
      TargetService targetService,
      String instanceId,
      String instanceUri,
      URI healthUri) {
    long startedAt = System.nanoTime();

    try {
      ResponseEntity<Void> response = restClient.get().uri(healthUri).retrieve().toBodilessEntity();
      long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
      return new DiscoveryProbeResponseDto(
          mode,
          targetService.key(),
          targetService.serviceId(),
          instanceId,
          instanceUri,
          response.getStatusCode().value(),
          response.getStatusCode().is2xxSuccessful(),
          durationMillis);
    } catch (RestClientException ex) {
      throw new ExternalServiceException(
          targetService.serviceId()
              + " health check failed through "
              + mode
              + ": "
              + ex.getMessage());
    }
  }

  private TargetService resolveTargetService(String serviceKey) {
    TargetService targetService = targetServices.get(serviceKey.toLowerCase(Locale.ROOT));
    if (targetService == null) {
      throw new BadRequestException("Unsupported service key: " + serviceKey);
    }
    return targetService;
  }

  private String stableInstanceKey(ServiceInstance instance) {
    String instanceId = instance.getInstanceId() == null ? "" : instance.getInstanceId();
    return instanceId + "|" + instance.getUri();
  }

  private record TargetService(String key, String serviceId) {}
}
