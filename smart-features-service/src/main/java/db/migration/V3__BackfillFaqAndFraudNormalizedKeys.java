package db.migration;

import com.pharmaflow.smartfeatures.util.TextSanitizer;
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

public class V3__BackfillFaqAndFraudNormalizedKeys extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    migrateFaq(connection);
    migrateFraudRule(connection);
  }

  private void migrateFaq(Connection connection) throws SQLException {
    String tableName = findExistingTableName(connection, "faq_entry");
    if (tableName == null) {
      return;
    }

    if (!columnExists(connection, tableName, "normalized_question")) {
      execute(
          connection, "ALTER TABLE " + tableName + " ADD COLUMN normalized_question VARCHAR(200)");
    }

    try (PreparedStatement select =
            connection.prepareStatement("SELECT faq_id, question FROM " + tableName);
        ResultSet resultSet = select.executeQuery();
        PreparedStatement update =
            connection.prepareStatement(
                "UPDATE " + tableName + " SET normalized_question = ? WHERE faq_id = ?")) {
      while (resultSet.next()) {
        update.setString(1, TextSanitizer.normalizeKey(resultSet.getString("question")));
        update.setLong(2, resultSet.getLong("faq_id"));
        update.addBatch();
      }
      update.executeBatch();
    }

    ensureNoDuplicateValues(
        connection,
        tableName,
        "normalized_question",
        "Cannot apply faq normalized_question uniqueness: duplicate normalized FAQ questions already exist. Clean up FAQ questions before rollout.");
    execute(
        connection, "ALTER TABLE " + tableName + " ALTER COLUMN normalized_question SET NOT NULL");
    if (!uniqueConstraintExists(connection, tableName, "uk_faq_entry_normalized_question")) {
      execute(
          connection,
          "ALTER TABLE "
              + tableName
              + " ADD CONSTRAINT uk_faq_entry_normalized_question UNIQUE (normalized_question)");
    }
  }

  private void migrateFraudRule(Connection connection) throws SQLException {
    String tableName = findExistingTableName(connection, "fraud_rule");
    if (tableName == null) {
      return;
    }

    if (!columnExists(connection, tableName, "normalized_rule_name")) {
      execute(
          connection, "ALTER TABLE " + tableName + " ADD COLUMN normalized_rule_name VARCHAR(100)");
    }

    try (PreparedStatement select =
            connection.prepareStatement("SELECT rule_id, rule_name FROM " + tableName);
        ResultSet resultSet = select.executeQuery();
        PreparedStatement update =
            connection.prepareStatement(
                "UPDATE " + tableName + " SET normalized_rule_name = ? WHERE rule_id = ?")) {
      while (resultSet.next()) {
        update.setString(1, TextSanitizer.normalizeKey(resultSet.getString("rule_name")));
        update.setLong(2, resultSet.getLong("rule_id"));
        update.addBatch();
      }
      update.executeBatch();
    }

    ensureNoDuplicateValues(
        connection,
        tableName,
        "normalized_rule_name",
        "Cannot apply fraud_rule normalized_rule_name uniqueness: duplicate normalized fraud rule names already exist. Clean up fraud rule names before rollout.");
    execute(
        connection, "ALTER TABLE " + tableName + " ALTER COLUMN normalized_rule_name SET NOT NULL");
    if (!uniqueConstraintExists(connection, tableName, "uk_fraud_rule_normalized_name")) {
      execute(
          connection,
          "ALTER TABLE "
              + tableName
              + " ADD CONSTRAINT uk_fraud_rule_normalized_name UNIQUE (normalized_rule_name)");
    }
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
