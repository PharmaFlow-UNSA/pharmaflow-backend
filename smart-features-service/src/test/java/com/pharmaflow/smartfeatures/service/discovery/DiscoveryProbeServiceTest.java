package com.pharmaflow.smartfeatures.service.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pharmaflow.smartfeatures.dto.discovery.DiscoveryProbeResponseDto;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DiscoveryProbeServiceTest {

  private LoadBalancerClient loadBalancerClient;
  private DiscoveryClient discoveryClient;
  private MockRestServiceServer server;
  private DiscoveryProbeService discoveryProbeService;

  @BeforeEach
  void setUp() {
    loadBalancerClient = org.mockito.Mockito.mock(LoadBalancerClient.class);
    discoveryClient = org.mockito.Mockito.mock(DiscoveryClient.class);
    RestClient.Builder restClientBuilder = RestClient.builder();
    server = MockRestServiceServer.bindTo(restClientBuilder).build();
    discoveryProbeService =
        new DiscoveryProbeService(
            loadBalancerClient,
            discoveryClient,
            restClientBuilder,
            "product-health-service",
            "order-prescription-service",
            "user-health-service");
  }

  @ParameterizedTest
  @MethodSource("serviceTargets")
  void loadBalancedProbeUsesDiscoveredInstanceHealthCheck(
      String serviceKey, String serviceId, String instanceId, int port) {
    when(loadBalancerClient.choose(serviceId))
        .thenReturn(new DefaultServiceInstance(instanceId, serviceId, "localhost", port, false));
    server
        .expect(requestTo("http://localhost:" + port + "/actuator/health"))
        .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

    DiscoveryProbeResponseDto response = discoveryProbeService.probe(serviceKey, "load-balanced");

    assertEquals("load-balanced", response.mode());
    assertEquals(serviceKey, response.serviceKey());
    assertEquals(serviceId, response.serviceId());
    assertEquals(instanceId, response.instanceId());
    assertEquals(200, response.httpStatus());
    assertTrue(response.healthy());
    server.verify();
  }

  @ParameterizedTest
  @MethodSource("serviceTargets")
  void directProbeUsesStableFirstDiscoveredInstance(
      String serviceKey, String serviceId, String ignoredInstanceId, int ignoredPort) {
    when(discoveryClient.getInstances(serviceId))
        .thenReturn(
            List.of(
                new DefaultServiceInstance(serviceId + "-b", serviceId, "localhost", 19082, false),
                new DefaultServiceInstance(
                    serviceId + "-a", serviceId, "localhost", 19081, false)));
    server
        .expect(requestTo("http://localhost:19081/actuator/health"))
        .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

    DiscoveryProbeResponseDto response = discoveryProbeService.probe(serviceKey, "direct");

    assertEquals("direct", response.mode());
    assertEquals(serviceKey, response.serviceKey());
    assertEquals(serviceId, response.serviceId());
    assertEquals(serviceId + "-a", response.instanceId());
    assertEquals(200, response.httpStatus());
    assertTrue(response.healthy());
    server.verify();
  }

  private static Stream<Arguments> serviceTargets() {
    return Stream.of(
        Arguments.of("product", "product-health-service", "product-1", 19081),
        Arguments.of("order", "order-prescription-service", "order-1", 19080),
        Arguments.of("user", "user-health-service", "user-1", 19082));
  }
}
