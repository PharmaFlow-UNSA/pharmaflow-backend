package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__AddFraudRuleCodesAndLogScores extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    String fraudRuleTable = findExistingTableName(connection, "fraud_rule");
    if (fraudRuleTable != null) {
      migrateFraudRule(connection, fraudRuleTable);
    }

    String fraudLogTable = findExistingTableName(connection, "fraud_log");
    if (fraudLogTable != null && !columnExists(connection, fraudLogTable, "score_contribution")) {
      execute(
          connection,
          "ALTER TABLE " + fraudLogTable + " ADD COLUMN score_contribution DOUBLE PRECISION");
    }
  }

  private void migrateFraudRule(Connection connection, String tableName) throws SQLException {
    if (!columnExists(connection, tableName, "rule_code")) {
      execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN rule_code VARCHAR(80)");
    }
    if (!columnExists(connection, tableName, "category")) {
      execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN category VARCHAR(50)");
    }

    try (PreparedStatement select =
            connection.prepareStatement("SELECT rule_id, rule_name FROM " + tableName);
        ResultSet resultSet = select.executeQuery();
        PreparedStatement update =
            connection.prepareStatement(
                "UPDATE " + tableName + " SET rule_code = ?, category = ? WHERE rule_id = ?")) {
      int legacyIndex = 1;
      while (resultSet.next()) {
        String ruleName = resultSet.getString("rule_name");
        update.setString(1, toLegacyRuleCode(ruleName, legacyIndex++));
        update.setString(2, "LEGACY");
        update.setLong(3, resultSet.getLong("rule_id"));
        update.addBatch();
      }
      update.executeBatch();
    }

    ensureNoDuplicateValues(
        connection,
        tableName,
        "rule_code",
        "Cannot apply fraud rule_code uniqueness: duplicate fraud rule codes already exist.");
    execute(connection, "ALTER TABLE " + tableName + " ALTER COLUMN rule_code SET NOT NULL");
    execute(connection, "ALTER TABLE " + tableName + " ALTER COLUMN category SET NOT NULL");
    if (!uniqueConstraintExists(connection, tableName, "uk_fraud_rule_code")) {
      execute(
          connection,
          "ALTER TABLE " + tableName + " ADD CONSTRAINT uk_fraud_rule_code UNIQUE (rule_code)");
    }
  }

  private String toLegacyRuleCode(String ruleName, int fallbackIndex) {
    String source =
        ruleName == null || ruleName.isBlank() ? "legacy_rule_" + fallbackIndex : ruleName;
    String normalized = source.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
    if (normalized.isBlank()) {
      return "LEGACY_RULE_" + fallbackIndex;
    }
    return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
  }

  private void ensureNoDuplicateValues(
      Connection connection, String tableName, String columnName, String message)
      throws SQLException {
    Set<String> values = new HashSet<>();
    try (PreparedStatement select =
            connection.prepareStatement(
                "SELECT "
                    + columnName
                    + " FROM "
                    + tableName
                    + " WHERE "
                    + columnName
                    + " IS NOT NULL");
        ResultSet resultSet = select.executeQuery()) {
      while (resultSet.next()) {
        String value = resultSet.getString(columnName);
        if (!values.add(value)) {
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

  private boolean columnExists(Connection connection, String tableName, String columnName)
      throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
      while (columns.next()) {
        if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
            && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
          return true;
        }
      }
      return false;
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
            "SELECT 1 FROM information_schema.table_constraints WHERE lower(table_name) = lower(?) AND lower(constraint_name) = lower(?)")) {
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
