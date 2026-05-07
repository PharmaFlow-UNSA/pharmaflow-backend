package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__AddSmartFeatureUniqueConstraints extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    addSearchItemConstraint(connection);
    addProductMatchConstraint(connection);
  }

  private void addSearchItemConstraint(Connection connection) throws SQLException {
    String tableName = findExistingTableName(connection, "symptom_search_item");
    if (tableName == null
        || uniqueConstraintExists(connection, tableName, "uk_symptom_search_item_search_symptom")) {
      return;
    }

    ensureNoDuplicatePairs(
        connection,
        tableName,
        "search_id",
        "symptom_id",
        "Cannot apply symptom_search_item uniqueness: duplicate search/symptom pairs already exist. Clean up data before rollout.");
    execute(
        connection,
        "ALTER TABLE "
            + tableName
            + " ADD CONSTRAINT uk_symptom_search_item_search_symptom UNIQUE (search_id, symptom_id)");
  }

  private void addProductMatchConstraint(Connection connection) throws SQLException {
    String tableName = findExistingTableName(connection, "symptom_product_match");
    if (tableName == null
        || uniqueConstraintExists(
            connection, tableName, "uk_symptom_product_match_symptom_product")) {
      return;
    }

    ensureNoDuplicatePairs(
        connection,
        tableName,
        "symptom_id",
        "product_id",
        "Cannot apply symptom_product_match uniqueness: duplicate symptom/product pairs already exist. Clean up data before rollout.");
    execute(
        connection,
        "ALTER TABLE "
            + tableName
            + " ADD CONSTRAINT uk_symptom_product_match_symptom_product UNIQUE (symptom_id, product_id)");
  }

  private void ensureNoDuplicatePairs(
      Connection connection,
      String tableName,
      String firstColumn,
      String secondColumn,
      String message)
      throws SQLException {
    Set<String> values = new HashSet<>();
    try (PreparedStatement select =
            connection.prepareStatement(
                "SELECT " + firstColumn + ", " + secondColumn + " FROM " + tableName);
        ResultSet resultSet = select.executeQuery()) {
      while (resultSet.next()) {
        String key = resultSet.getString(firstColumn) + ":" + resultSet.getString(secondColumn);
        if (!values.add(key)) {
          throw new FlywayException(message);
        }
      }
    }
  }

  private String findExistingTableName(Connection connection, String expectedTableName)
      throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    try (ResultSet tables = metaData.getTables(null, null, null, new String[] {"TABLE"})) {
      while (tables.next()) {
        String actualTableName = tables.getString("TABLE_NAME");
        if (expectedTableName.equalsIgnoreCase(actualTableName)) {
          return actualTableName;
        }
      }
      return null;
    }
  }

  private boolean uniqueConstraintExists(
      Connection connection, String tableName, String constraintName) throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, true, false)) {
      while (indexes.next()) {
        String indexName = indexes.getString("INDEX_NAME");
        if (indexName != null && constraintName.equalsIgnoreCase(indexName)) {
          return true;
        }
      }
    }
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT 1 FROM information_schema.table_constraints WHERE lower(table_name) = lower(?) AND lower(constraint_name) = lower(?)"); ) {
      statement.setString(1, tableName);
      statement.setString(2, constraintName);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
