package db.migration;

import com.pharmaflow.smartfeatures.util.SymptomTextNormalizer;
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

public class V2__BackfillSymptomNormalizedName extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    String tableName = findExistingTableName(connection, "symptom");
    if (tableName == null) {
      return;
    }

    if (!columnExists(connection, tableName, "normalized_name")) {
      execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN normalized_name VARCHAR(100)");
    }

    backfillNormalizedNames(connection, tableName);
    ensureNoDuplicateNormalizedNames(connection, tableName);
    execute(connection, "ALTER TABLE " + tableName + " ALTER COLUMN normalized_name SET NOT NULL");

    if (!uniqueConstraintExists(connection, tableName, "uk_symptom_normalized_name")) {
      execute(
          connection,
          "ALTER TABLE "
              + tableName
              + " ADD CONSTRAINT uk_symptom_normalized_name UNIQUE (normalized_name)");
    }
  }

  private void backfillNormalizedNames(Connection connection, String tableName)
      throws SQLException {
    try (PreparedStatement select =
            connection.prepareStatement("SELECT symptom_id, name FROM " + tableName);
        ResultSet resultSet = select.executeQuery();
        PreparedStatement update =
            connection.prepareStatement(
                "UPDATE " + tableName + " SET normalized_name = ? WHERE symptom_id = ?")) {
      while (resultSet.next()) {
        String normalizedName = SymptomTextNormalizer.normalizeName(resultSet.getString("name"));
        update.setString(1, normalizedName);
        update.setLong(2, resultSet.getLong("symptom_id"));
        update.addBatch();
      }
      update.executeBatch();
    }
  }

  private void ensureNoDuplicateNormalizedNames(Connection connection, String tableName)
      throws SQLException {
    Set<String> normalizedNames = new HashSet<>();
    try (PreparedStatement select =
            connection.prepareStatement(
                "SELECT normalized_name FROM " + tableName + " WHERE normalized_name IS NOT NULL");
        ResultSet resultSet = select.executeQuery()) {
      while (resultSet.next()) {
        String normalizedName = resultSet.getString("normalized_name");
        if (!normalizedNames.add(normalizedName)) {
          throw new FlywayException(
              "Cannot apply symptom normalized_name uniqueness: duplicate normalized symptom names already exist. Clean up symptom names before rollout.");
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
