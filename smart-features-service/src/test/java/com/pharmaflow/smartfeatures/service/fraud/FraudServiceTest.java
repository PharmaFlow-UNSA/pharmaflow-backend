package com.pharmaflow.smartfeatures.service.fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.smartfeatures.dto.fraud.FraudCheckRequestDto;
import com.pharmaflow.smartfeatures.enums.fraud.FraudDecision;
import com.pharmaflow.smartfeatures.enums.fraud.FraudEventType;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudCheckRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudLogRepository;
import com.pharmaflow.smartfeatures.repositories.fraud.FraudRuleRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(roles = "PHARMACIST")
class FraudServiceTest {

  private static final LocalFraudApiServer API_SERVER = new LocalFraudApiServer();

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    API_SERVER.start();
    registry.add("spring.cloud.discovery.enabled", () -> "true");
    registry.add(
        "spring.cloud.discovery.client.simple.instances.order-prescription-service[0].uri",
        API_SERVER::baseUrl);
    registry.add(
        "spring.cloud.discovery.client.simple.instances.product-health-service[0].uri",
        API_SERVER::baseUrl);
    registry.add(
        "spring.cloud.discovery.client.simple.instances.user-health-service[0].uri",
        API_SERVER::baseUrl);
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private FraudCheckRepository fraudCheckRepository;

  @Autowired private FraudLogRepository fraudLogRepository;

  @Autowired private FraudRuleRepository fraudRuleRepository;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void setUp() {
    API_SERVER.reset();
    fraudLogRepository.deleteAll();
    fraudCheckRepository.deleteAll();
    fraudRuleRepository.deleteAll();
  }

  @AfterAll
  static void tearDown() {
    API_SERVER.stop();
  }

  @Test
  void createCheckShouldEvaluateHighRiskOrderAgainstRealUpstreamResponses() throws Exception {
    mockMvc
        .perform(
            post("/api/fraud-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FraudCheckRequestDto(100L))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.orderId").value(100))
        .andExpect(jsonPath("$.riskScore").value(100.0))
        .andExpect(jsonPath("$.decision").value(FraudDecision.BLOCKED.name()));

    assertThat(fraudCheckRepository.findAll()).hasSize(1);
    assertThat(fraudLogRepository.findAll()).hasSize(12);
    assertThat(fraudLogRepository.findAll())
        .filteredOn(log -> log.getEventType() == FraudEventType.TRIGGERED)
        .hasSizeGreaterThan(5);
    assertThat(fraudLogRepository.findAll())
        .filteredOn(log -> log.getEventType() == FraudEventType.SKIPPED)
        .anySatisfy(log -> assertThat(log.getDetails()).contains("account createdAt"))
        .anySatisfy(log -> assertThat(log.getDetails()).contains("login or device-change"));
  }

  @Test
  void createCheckShouldApproveLowRiskOrderWithoutInventingRisk() throws Exception {
    mockMvc
        .perform(
            post("/api/fraud-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FraudCheckRequestDto(900L))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(9))
        .andExpect(jsonPath("$.orderId").value(900))
        .andExpect(jsonPath("$.riskScore").value(0.0))
        .andExpect(jsonPath("$.decision").value(FraudDecision.APPROVED.name()));

    assertThat(fraudLogRepository.findAll())
        .filteredOn(log -> log.getEventType() == FraudEventType.TRIGGERED)
        .isEmpty();
    assertThat(fraudLogRepository.findAll())
        .filteredOn(log -> log.getEventType() == FraudEventType.SKIPPED)
        .isNotEmpty();
  }

  @Test
  void createCheckShouldReturnNotFoundWhenUpstreamOrderDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/api/fraud-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FraudCheckRequestDto(404L))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void createCheckShouldReturnUnavailableWhenRequiredProductSignalFails() throws Exception {
    API_SERVER.setProductUnavailable(true);

    mockMvc
        .perform(
            post("/api/fraud-checks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FraudCheckRequestDto(100L))))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.errorCode").value("EXTERNAL_SERVICE_UNAVAILABLE"));
  }

  private static class LocalFraudApiServer {

    private HttpServer server;
    private volatile boolean productUnavailable;

    void start() {
      if (server != null) {
        return;
      }
      try {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
      } catch (IOException ex) {
        throw new IllegalStateException("Failed to start local fraud API server", ex);
      }
    }

    String baseUrl() {
      return "http://localhost:" + server.getAddress().getPort();
    }

    void reset() {
      productUnavailable = false;
    }

    void setProductUnavailable(boolean productUnavailable) {
      this.productUnavailable = productUnavailable;
    }

    void stop() {
      if (server != null) {
        server.stop(0);
        server = null;
      }
    }

    private void handle(HttpExchange exchange) throws IOException {
      if (!"GET".equals(exchange.getRequestMethod())) {
        respond(exchange, 405, "{}");
        return;
      }

      String path = exchange.getRequestURI().getPath();
      if (productUnavailable && path.startsWith("/api/products/")) {
        respond(exchange, 503, "{\"message\":\"product service down\"}");
        return;
      }

      switch (path) {
        case "/api/orders/100" -> respond(exchange, 200, highRiskOrder());
        case "/api/orders/900" -> respond(exchange, 200, lowRiskOrder());
        case "/api/orders/404" -> respond(exchange, 404, "{}");
        case "/api/orders/user/1" -> respond(exchange, 200, highRiskUserOrders());
        case "/api/orders/user/9" -> respond(exchange, 200, lowRiskUserOrders());
        case "/api/orders" -> respond(exchange, 200, allOrders());
        case "/api/prescriptions/user/1" -> respond(exchange, 200, highRiskUserPrescriptions());
        case "/api/prescriptions/user/9" -> respond(exchange, 200, "[]");
        case "/api/prescriptions" -> respond(exchange, 200, allPrescriptions());
        case "/api/products/100" -> respond(exchange, 200, warfarinProduct());
        case "/api/products/101" -> respond(exchange, 200, ibuprofenProduct());
        case "/api/products/200" -> respond(exchange, 200, vitaminProduct());
        case "/api/users/1" -> respond(exchange, 200, user(1));
        case "/api/users/9" -> respond(exchange, 200, user(9));
        default -> respond(exchange, 404, "{}");
      }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(bytes);
      }
    }

    private String highRiskOrder() {
      return """
                    {
                      "id": 100,
                      "userId": 1,
                      "status": "PENDING",
                      "totalAmount": 260.50,
                      "shippingAddress": "Shared Risk Street 1",
                      "createdAt": "2026-05-05T10:00:00",
                      "prescriptionId": 500,
                      "payment": {"id": 1000, "status": "FAILED"},
                      "orderItems": [
                        {"id": 1, "productId": 100, "quantity": 12, "unitPrice": 18.00},
                        {"id": 2, "productId": 101, "quantity": 2, "unitPrice": 22.25}
                      ]
                    }
                    """;
    }

    private String lowRiskOrder() {
      return """
                    {
                      "id": 900,
                      "userId": 9,
                      "status": "COMPLETED",
                      "totalAmount": 9.99,
                      "shippingAddress": "Calm Street 9",
                      "createdAt": "2026-05-05T08:00:00",
                      "payment": {"id": 9000, "status": "PAID"},
                      "orderItems": [
                        {"id": 9, "productId": 200, "quantity": 1, "unitPrice": 9.99}
                      ]
                    }
                    """;
    }

    private String highRiskUserOrders() {
      return """
                    [
                      %s,
                      {
                        "id": 101,
                        "userId": 1,
                        "status": "FAILED",
                        "totalAmount": 110.00,
                        "shippingAddress": "Shared Risk Street 1",
                        "createdAt": "2026-05-05T09:35:00",
                        "payment": {"id": 1001, "status": "FAILED"},
                        "orderItems": [{"id": 3, "productId": 100, "quantity": 1, "unitPrice": 18.00}]
                      },
                      {
                        "id": 102,
                        "userId": 1,
                        "status": "FAILED",
                        "totalAmount": 120.00,
                        "shippingAddress": "Shared Risk Street 1",
                        "createdAt": "2026-05-05T09:10:00",
                        "payment": {"id": 1002, "status": "FAILED"},
                        "orderItems": [{"id": 4, "productId": 100, "quantity": 1, "unitPrice": 18.00}]
                      }
                    ]
                    """
          .formatted(highRiskOrder());
    }

    private String lowRiskUserOrders() {
      return "[" + lowRiskOrder() + "]";
    }

    private String allOrders() {
      return """
                    [
                      %s,
                      {
                        "id": 201,
                        "userId": 2,
                        "status": "COMPLETED",
                        "totalAmount": 20.00,
                        "shippingAddress": "Shared Risk Street 1",
                        "createdAt": "2026-05-04T12:00:00",
                        "payment": {"id": 2001, "status": "PAID"},
                        "orderItems": [{"id": 5, "productId": 200, "quantity": 1, "unitPrice": 20.00}]
                      },
                      {
                        "id": 202,
                        "userId": 3,
                        "status": "COMPLETED",
                        "totalAmount": 21.00,
                        "shippingAddress": "Shared Risk Street 1",
                        "createdAt": "2026-05-04T13:00:00",
                        "payment": {"id": 2002, "status": "PAID"},
                        "orderItems": [{"id": 6, "productId": 200, "quantity": 1, "unitPrice": 21.00}]
                      },
                      %s
                    ]
                    """
          .formatted(highRiskOrder(), lowRiskOrder());
    }

    private String highRiskUserPrescriptions() {
      return """
                    [
                      {"id": 500, "userId": 1, "status": "REJECTED", "imageUrl": "https://docs/presc-500.png", "uploadedAt": "2026-05-04T09:00:00", "reviewedAt": "2026-05-04T10:00:00"},
                      {"id": 501, "userId": 1, "status": "REJECTED", "imageUrl": "https://docs/presc-501.png", "uploadedAt": "2026-05-03T09:00:00", "reviewedAt": "2026-05-03T10:00:00"},
                      {"id": 502, "userId": 1, "status": "REJECTED", "imageUrl": "https://docs/presc-502.png", "uploadedAt": "2026-05-02T09:00:00", "reviewedAt": "2026-05-02T10:00:00"}
                    ]
                    """;
    }

    private String allPrescriptions() {
      return """
                    [
                      {"id": 500, "userId": 1, "status": "REJECTED", "imageUrl": "https://docs/presc-500.png", "uploadedAt": "2026-05-04T09:00:00", "reviewedAt": "2026-05-04T10:00:00"},
                      {"id": 600, "userId": 2, "status": "APPROVED", "imageUrl": "https://docs/presc-500.png", "uploadedAt": "2026-05-04T11:00:00", "reviewedAt": "2026-05-04T12:00:00"},
                      {"id": 700, "userId": 3, "status": "APPROVED", "imageUrl": "https://docs/other.png", "uploadedAt": "2026-05-04T11:00:00", "reviewedAt": "2026-05-04T12:00:00"}
                    ]
                    """;
    }

    private String warfarinProduct() {
      return """
                    {
                      "id": 100,
                      "name": "Warfarin Control",
                      "brandName": "Anticoagulant Rx",
                      "description": "Controlled anticoagulant therapy",
                      "productType": "RX",
                      "requiresPrescription": true,
                      "category": {"id": 10, "name": "Anticoagulants", "description": "Blood thinner medication"},
                      "substances": [
                        {"id": 1, "inn": "Warfarin", "commonName": "Warfarin", "atcCode": "B01AA03", "description": "anticoagulant"}
                      ]
                    }
                    """;
    }

    private String ibuprofenProduct() {
      return """
                    {
                      "id": 101,
                      "name": "Ibuprofen Forte",
                      "brandName": "Brufen",
                      "description": "NSAID pain relief",
                      "productType": "OTC",
                      "requiresPrescription": false,
                      "category": {"id": 11, "name": "NSAID", "description": "Anti-inflammatory"},
                      "substances": [
                        {"id": 2, "inn": "Ibuprofen", "commonName": "Ibuprofen", "atcCode": "M01AE01", "description": "nsaid"}
                      ]
                    }
                    """;
    }

    private String vitaminProduct() {
      return """
                    {
                      "id": 200,
                      "name": "Vitamin C",
                      "brandName": "Daily C",
                      "description": "Vitamin supplement",
                      "productType": "SUPPLEMENT",
                      "requiresPrescription": false,
                      "category": {"id": 12, "name": "Vitamins", "description": "Supplements"},
                      "substances": [
                        {"id": 3, "inn": "Ascorbic acid", "commonName": "Vitamin C", "atcCode": "A11GA01", "description": "vitamin"}
                      ]
                    }
                    """;
    }

    private String user(long id) {
      return """
                    {"id":%d,"firstName":"Test","lastName":"User","email":"user%d@example.com"}
                    """
          .formatted(id, id);
    }
  }
}
