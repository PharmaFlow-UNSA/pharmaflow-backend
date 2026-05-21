package com.pharmaflow.smartfeatures.controller.productintelligence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.smartfeatures.client.product.ProductCatalogClient;
import com.pharmaflow.smartfeatures.dto.product.ProductSnapshot;
import com.pharmaflow.smartfeatures.dto.product.ProductSubstituteSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@SpringBootTest(properties = "smartfeatures.embedding.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("real-integration")
@EnabledIfEnvironmentVariable(
    named = "SMARTFEATURES_RUN_REAL_PRODUCT_INTELLIGENCE_TESTS",
    matches = "true")
class ProductIntelligenceRealIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ProductCatalogClient productCatalogClient;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired
  @Qualifier("loadBalancedRestClientBuilder")
  private RestClient.Builder loadBalancedRestClientBuilder;

  @Value("${smartfeatures.user-service.service-id:user-health-service}")
  private String userServiceId;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void symptomSearchShouldRankRealProductSnapshotsAndUseEmbeddingFallback() throws Exception {
    ProductSnapshot product = firstActiveProduct();
    Long userId = createExternalUser();
    String suffix = UUID.randomUUID().toString();
    Long symptomId = null;
    Long searchId = null;

    try {
      String symptomResponse =
          mockMvc
              .perform(
                  post("/api/symptoms")
                      .contentType("application/json")
                      .content(
                          """
                                    {
                                      "name": "Integration Cough %s",
                                      "description": "Real product-health snapshot ranking",
                                      "severityLevel": "MEDIUM",
                                      "isActive": true,
                                      "tags": ["%s"]
                                    }
                                    """
                              .formatted(suffix, firstToken(product.getName()))))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      symptomId = objectMapper.readTree(symptomResponse).get("id").asLong();

      mockMvc
          .perform(
              post("/api/symptoms/{symptomId}/matches", symptomId)
                  .contentType("application/json")
                  .content(
                      """
                                    {
                                      "productId": %d,
                                      "relevanceScore": 0.9,
                                      "matchReason": "Real product-health candidate"
                                    }
                                    """
                          .formatted(product.getId())))
          .andExpect(status().isCreated());

      String searchResponse =
          mockMvc
              .perform(
                  post("/api/symptom-searches")
                      .contentType("application/json")
                      .content(
                          """
                                    {
                                      "userId": %d,
                                      "searchQuery": "integration cough %s"
                                    }
                                    """
                              .formatted(userId, suffix)))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      searchId = objectMapper.readTree(searchResponse).get("id").asLong();

      mockMvc
          .perform(
              post("/api/symptom-searches/{id}/items", searchId)
                  .contentType("application/json")
                  .content(
                      """
                                    {"symptomId": %d}
                                    """
                          .formatted(symptomId)))
          .andExpect(status().isCreated());

      mockMvc
          .perform(get("/api/symptom-searches/{id}/matches", searchId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].productId").value(product.getId()))
          .andExpect(jsonPath("$[0].matchReason", containsString("embedding skipped")));
    } finally {
      cleanSymptomSearch(searchId);
      cleanSymptom(symptomId);
      deleteExternalUser(userId);
    }
  }

  @Test
  void generateSimilarProductsShouldPersistActiveRecommendationsFromRealProductSnapshots()
      throws Exception {
    ProductPair pair = findSimilarPair();
    Long userId = createExternalUser();

    try {
      mockMvc
          .perform(
              post("/api/recommendations/generate")
                  .contentType("application/json")
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "userId",
                              userId,
                              "recommendationType",
                              "SIMILAR_PRODUCT",
                              "seedProductId",
                              pair.seed().getId(),
                              "limit",
                              5))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.length()").isNotEmpty())
          .andExpect(jsonPath("$[0].recommendationType").value("SIMILAR_PRODUCT"))
          .andExpect(jsonPath("$[0].reasonText", containsString("embedding skipped")));

      Integer activeCount =
          jdbcTemplate.queryForObject(
              """
                    SELECT COUNT(*)
                    FROM recommendation
                    WHERE user_id = ? AND recommendation_type = 'SIMILAR_PRODUCT' AND status = 'ACTIVE'
                    """,
              Integer.class,
              userId);
      assertThat(activeCount).isGreaterThan(0);
    } finally {
      cleanRecommendations(userId);
      deleteExternalUser(userId);
    }
  }

  @Test
  void generateAlternativesShouldUseRealProductHealthSubstitutes() throws Exception {
    ProductPair pair = findSubstitutePair();
    Long userId = createExternalUser();

    try {
      mockMvc
          .perform(
              post("/api/recommendations/generate")
                  .contentType("application/json")
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "userId",
                              userId,
                              "recommendationType",
                              "ALTERNATIVE",
                              "seedProductId",
                              pair.seed().getId(),
                              "limit",
                              5))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$[0].productId").value(pair.candidate().getId()))
          .andExpect(jsonPath("$[0].reasonText", containsString("known product substitute")));

      Integer activeCount =
          jdbcTemplate.queryForObject(
              """
                    SELECT COUNT(*)
                    FROM recommendation
                    WHERE user_id = ? AND recommendation_type = 'ALTERNATIVE' AND status = 'ACTIVE'
                    """,
              Integer.class,
              userId);
      assertThat(activeCount).isGreaterThan(0);
    } finally {
      cleanRecommendations(userId);
      deleteExternalUser(userId);
    }
  }

  private Long createExternalUser() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String response =
        userHealthRestClient()
            .post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                        {
                          "firstName": "Smart",
                          "lastName": "Integration",
                          "email": "smart-%s@example.com",
                          "password": "Password1!",
                          "patientProfile": {
                            "weight": 75.0,
                            "height": 180.0,
                            "bloodType": "O+",
                            "allergies": [],
                            "therapies": []
                          }
                        }
                        """
                    .formatted(suffix))
            .retrieve()
            .body(String.class);
    return objectMapper.readTree(response).get("id").asLong();
  }

  private void deleteExternalUser(Long userId) {
    if (userId == null) {
      return;
    }
    try {
      userHealthRestClient().delete().uri("/api/users/{id}", userId).retrieve().toBodilessEntity();
    } catch (RestClientException ignored) {
      // Best-effort cleanup for an external integration environment.
    }
  }

  private RestClient userHealthRestClient() {
    return loadBalancedRestClientBuilder.clone().baseUrl("http://" + userServiceId).build();
  }

  private ProductSnapshot firstActiveProduct() {
    List<ProductSnapshot> products = activeProducts();
    assumeTrue(!products.isEmpty(), "product-health has no active products");
    return products.get(0);
  }

  private ProductPair findSimilarPair() {
    List<ProductSnapshot> products = activeProducts();
    Optional<ProductPair> pair =
        products.stream()
            .flatMap(
                seed ->
                    products.stream()
                        .filter(candidate -> !seed.getId().equals(candidate.getId()))
                        .filter(
                            candidate ->
                                sameCategory(seed, candidate) || sharesSubstance(seed, candidate))
                        .map(candidate -> new ProductPair(seed, candidate)))
            .findFirst();
    assumeTrue(
        pair.isPresent(),
        "product-health has no active products with shared category or substance");
    return pair.get();
  }

  private ProductPair findSubstitutePair() {
    List<ProductSnapshot> products = activeProducts();
    Map<Long, ProductSnapshot> productsById =
        products.stream().collect(Collectors.toMap(ProductSnapshot::getId, product -> product));
    Optional<ProductPair> pair =
        products.stream()
            .sorted(Comparator.comparing(ProductSnapshot::getId))
            .flatMap(
                seed ->
                    productCatalogClient.getSubstitutesForProduct(seed.getId()).stream()
                        .map(ProductSubstituteSnapshot::getSubstituteProductId)
                        .filter(productsById::containsKey)
                        .map(substituteId -> new ProductPair(seed, productsById.get(substituteId))))
            .findFirst();
    assumeTrue(pair.isPresent(), "product-health has no active substitute relation");
    return pair.get();
  }

  private List<ProductSnapshot> activeProducts() {
    return productCatalogClient.getActiveProducts().stream()
        .filter(product -> product.getId() != null)
        .filter(product -> !Boolean.FALSE.equals(product.getIsActive()))
        .toList();
  }

  private boolean sameCategory(ProductSnapshot left, ProductSnapshot right) {
    return left.getCategory() != null
        && right.getCategory() != null
        && left.getCategory().getId() != null
        && left.getCategory().getId().equals(right.getCategory().getId());
  }

  private boolean sharesSubstance(ProductSnapshot left, ProductSnapshot right) {
    Set<Long> leftSubstances =
        left.getSubstances() == null
            ? Set.of()
            : left.getSubstances().stream()
                .map(substance -> substance.getId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    if (leftSubstances.isEmpty() || right.getSubstances() == null) {
      return false;
    }
    return right.getSubstances().stream()
        .map(substance -> substance.getId())
        .anyMatch(leftSubstances::contains);
  }

  private String firstToken(String value) {
    if (value == null || value.isBlank()) {
      return "product";
    }
    return value.split("[^\\p{L}\\p{N}]+")[0];
  }

  private void cleanRecommendations(Long userId) {
    jdbcTemplate.update(
        """
                DELETE FROM recommendation_event
                WHERE recommendation_id IN (
                    SELECT recommendation_id FROM recommendation WHERE user_id = ?
                )
                """,
        userId);
    jdbcTemplate.update("DELETE FROM recommendation WHERE user_id = ?", userId);
  }

  private void cleanSymptomSearch(Long searchId) {
    if (searchId == null) {
      return;
    }
    jdbcTemplate.update("DELETE FROM symptom_search_item WHERE search_id = ?", searchId);
    jdbcTemplate.update("DELETE FROM symptom_search WHERE search_id = ?", searchId);
  }

  private void cleanSymptom(Long symptomId) {
    if (symptomId == null) {
      return;
    }
    jdbcTemplate.update("DELETE FROM symptom_product_match WHERE symptom_id = ?", symptomId);
    jdbcTemplate.update("DELETE FROM symptom_tag WHERE symptom_id = ?", symptomId);
    jdbcTemplate.update("DELETE FROM symptom WHERE symptom_id = ?", symptomId);
  }

  private record ProductPair(ProductSnapshot seed, ProductSnapshot candidate) {}
}
