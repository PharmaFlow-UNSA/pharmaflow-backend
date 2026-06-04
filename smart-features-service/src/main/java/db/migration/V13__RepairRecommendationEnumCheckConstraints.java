package db.migration;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationEventType;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__RepairRecommendationEnumCheckConstraints extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();

    repairCheckConstraint(
        connection,
        "recommendation",
        "recommendation_recommendation_type_check",
        "recommendation_type",
        enumValues(RecommendationType.values()));
    repairCheckConstraint(
        connection,
        "recommendation_event",
        "recommendation_event_event_type_check",
        "event_type",
        enumValues(RecommendationEventType.values()));
  }

  private void repairCheckConstraint(
      Connection connection,
      String expectedTableName,
      String constraintName,
      String columnName,
      String allowedValues)
      throws SQLException {
    String tableName = findExistingTableName(connection, expectedTableName);
    if (tableName == null) {
      return;
    }

    if (constraintExists(connection, tableName, constraintName)) {
      execute(connection, "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
    }

    execute(
        connection,
        "ALTER TABLE "
            + tableName
            + " ADD CONSTRAINT "
            + constraintName
            + " CHECK ("
            + columnName
            + " IN ("
            + allowedValues
            + "))");
  }

  private String enumValues(Enum<?>[] values) {
    return Arrays.stream(values)
        .map(value -> "'" + value.name().replace("'", "''") + "'")
        .collect(Collectors.joining(", "));
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

  private boolean constraintExists(Connection connection, String tableName, String constraintName)
      throws SQLException {
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
