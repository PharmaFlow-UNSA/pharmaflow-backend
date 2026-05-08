package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V9__RepairFraudSchemaForLegacyDatabases extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();

    String fraudLogTable = findExistingTableName(connection, "fraud_log");
    if (fraudLogTable != null && !columnExists(connection, fraudLogTable, "score_contribution")) {
      execute(
          connection,
          "ALTER TABLE " + fraudLogTable + " ADD COLUMN score_contribution DOUBLE PRECISION");
    }

    String fraudRuleTable = findExistingTableName(connection, "fraud_rule");
    if (fraudRuleTable != null) {
      if (!columnExists(connection, fraudRuleTable, "rule_code")) {
        execute(connection, "ALTER TABLE " + fraudRuleTable + " ADD COLUMN rule_code VARCHAR(80)");
      }
      if (!columnExists(connection, fraudRuleTable, "category")) {
        execute(connection, "ALTER TABLE " + fraudRuleTable + " ADD COLUMN category VARCHAR(50)");
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

  private void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
