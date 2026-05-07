package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__AddFaqEmbeddingVector extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    if (!isPostgres(connection)) {
      return;
    }

    String tableName = findExistingTableName(connection, "faq_entry");
    if (tableName == null || columnExists(connection, tableName, "embedding")) {
      return;
    }

    execute(connection, "CREATE EXTENSION IF NOT EXISTS vector");
    execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN embedding vector(384)");
  }

  private boolean isPostgres(Connection connection) throws SQLException {
    return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
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
