package com.pharmaflow.smartfeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartFeaturesServiceApplicationTests {

  @Autowired private DataSource dataSource;

  @Autowired private HealthEndpoint healthEndpoint;

  @Test
  void contextLoads() {}

  @Test
  void actuatorHealthEndpointReportsUp() {
    assertNotNull(healthEndpoint);
    assertEquals(Status.UP, healthEndpoint.health().getStatus());
  }

  @Test
  void databaseConnectionExistsAndTablesArePresent() throws SQLException {
    assertNotNull(dataSource);

    try (Connection connection = dataSource.getConnection()) {
      assertNotNull(connection);
      assertTrue(connection.isValid(2));

      Set<String> tableNames = getTableNames(connection.getMetaData());

      assertFalse(tableNames.isEmpty());
      assertTrue(tableNames.contains("symptom"));
      assertTrue(tableNames.contains("symptom_search"));
      assertTrue(tableNames.contains("symptom_search_item"));
      assertTrue(tableNames.contains("symptom_product_match"));
      assertTrue(tableNames.contains("recommendation"));
      assertTrue(tableNames.contains("recommendation_event"));
      assertTrue(tableNames.contains("therapy_reminder"));
      assertTrue(tableNames.contains("notification"));
      assertTrue(tableNames.contains("notification_trigger"));
      assertTrue(tableNames.contains("chat_session"));
      assertTrue(tableNames.contains("chat_message"));
      assertTrue(tableNames.contains("faq_entry"));
      assertTrue(tableNames.contains("chat_intent_match"));
      assertTrue(tableNames.contains("fraud_check"));
      assertTrue(tableNames.contains("fraud_rule"));
      assertTrue(tableNames.contains("fraud_log"));
    }
  }

  private Set<String> getTableNames(DatabaseMetaData metaData) throws SQLException {
    Set<String> tableNames = new HashSet<>();

    try (ResultSet tables = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (tables.next()) {
        tableNames.add(tables.getString("TABLE_NAME").toLowerCase());
      }
    }

    return tableNames;
  }
}
