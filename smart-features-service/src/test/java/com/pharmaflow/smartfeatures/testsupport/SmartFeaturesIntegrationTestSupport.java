package com.pharmaflow.smartfeatures.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "smartfeatures.embedding.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class SmartFeaturesIntegrationTestSupport {

  private static final LocalProductHealthApiServer PRODUCT_HEALTH_API_SERVER =
      new LocalProductHealthApiServer();

  @DynamicPropertySource
  static void registerExternalServiceInstances(DynamicPropertyRegistry registry) {
    PRODUCT_HEALTH_API_SERVER.start();
    registry.add("spring.cloud.discovery.enabled", () -> "true");
    registry.add(
        "spring.cloud.discovery.client.simple.instances.product-health-service[0].uri",
        PRODUCT_HEALTH_API_SERVER::baseUrl);
  }

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetSmartFeaturesDatabase() {
    jdbcTemplate.update("DELETE FROM recommendation_event");
    jdbcTemplate.update("DELETE FROM recommendation");
    jdbcTemplate.update("DELETE FROM symptom_search_item");
    jdbcTemplate.update("DELETE FROM symptom_product_match");
    jdbcTemplate.update("DELETE FROM symptom_search");
    jdbcTemplate.update("DELETE FROM symptom_tag");
    jdbcTemplate.update("DELETE FROM symptom");
    jdbcTemplate.update("DELETE FROM notification_trigger");
    jdbcTemplate.update("DELETE FROM notification");
    jdbcTemplate.update("DELETE FROM therapy_reminder");
    jdbcTemplate.update("DELETE FROM fraud_log");
    jdbcTemplate.update("DELETE FROM fraud_check");
    jdbcTemplate.update("DELETE FROM fraud_rule");
    jdbcTemplate.update("DELETE FROM chat_intent_match");
    jdbcTemplate.update("DELETE FROM chat_message");
    jdbcTemplate.update("DELETE FROM chat_session");
    jdbcTemplate.update("DELETE FROM faq_entry");
  }

  protected String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  protected Long responseId(String responseBody) throws Exception {
    return objectMapper.readTree(responseBody).get("id").asLong();
  }

  private static class LocalProductHealthApiServer {

    private HttpServer server;

    synchronized void start() {
      if (server != null) {
        return;
      }
      try {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(
            Executors.newSingleThreadExecutor(
                runnable -> {
                  Thread thread = new Thread(runnable, "local-product-health-api");
                  thread.setDaemon(true);
                  return thread;
                }));
        server.start();
      } catch (IOException ex) {
        throw new IllegalStateException("Failed to start local product-health API server", ex);
      }
    }

    String baseUrl() {
      return "http://localhost:" + server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
      if (!"GET".equals(exchange.getRequestMethod())) {
        respond(exchange, 405, "{}");
        return;
      }

      String path = exchange.getRequestURI().getPath();
      if ("/api/products".equals(path)) {
        respond(exchange, 200, "[" + product101() + "]");
        return;
      }
      if ("/api/products/101".equals(path)) {
        respond(exchange, 200, product101());
        return;
      }
      if ("/api/substitutes/product/101".equals(path)) {
        respond(exchange, 200, "[]");
        return;
      }

      respond(exchange, 404, "{}");
    }

    private String product101() {
      return """
                    {
                      "id": 101,
                      "name": "Cough Relief Syrup",
                      "barcode": "TEST-101",
                      "description": "Relieves dry cough symptoms",
                      "price": 8.50,
                      "brandName": "PharmaFlow",
                      "manufacturer": "PharmaFlow Labs",
                      "requiresPrescription": false,
                      "productType": "MEDICINE",
                      "imageUrl": null,
                      "isActive": true,
                      "packageSize": "100ml",
                      "category": {
                        "id": 1,
                        "name": "Cough and cold",
                        "description": "Cough relief products",
                        "parentCategoryId": null
                      },
                      "substances": []
                    }
                    """;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(bytes);
      }
    }
  }
}
