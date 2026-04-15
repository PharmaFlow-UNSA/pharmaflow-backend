package db.migration;

import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationStatus;
import com.pharmaflow.smartfeatures.enums.recommendation.RecommendationType;
import com.pharmaflow.smartfeatures.util.RecommendationKeyNormalizer;
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

public class V5__AddRecommendationActiveDimensionKey extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = findExistingTableName(connection, "recommendation");
        if (tableName == null) {
            return;
        }

        if (!columnExists(connection, tableName, "active_dimension_key")) {
            execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN active_dimension_key VARCHAR(200)");
        }

        backfillActiveDimensionKey(connection, tableName);
        ensureNoDuplicateActiveDimensionKeys(connection, tableName);
        if (!uniqueConstraintExists(connection, tableName, "uk_recommendation_active_dimension_key")) {
            execute(
                    connection,
                    "ALTER TABLE " + tableName
                            + " ADD CONSTRAINT uk_recommendation_active_dimension_key UNIQUE (active_dimension_key)");
        }
    }

    private void backfillActiveDimensionKey(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                        "SELECT recommendation_id, user_id, patient_profile_id, product_id, recommendation_type, status FROM "
                                + tableName);
                ResultSet resultSet = select.executeQuery();
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE " + tableName + " SET active_dimension_key = ? WHERE recommendation_id = ?")) {
            while (resultSet.next()) {
                RecommendationType recommendationType =
                        RecommendationType.valueOf(resultSet.getString("recommendation_type"));
                RecommendationStatus status = RecommendationStatus.valueOf(resultSet.getString("status"));
                Long patientProfileId = resultSet.getObject("patient_profile_id") == null
                        ? null
                        : resultSet.getLong("patient_profile_id");

                String key = RecommendationKeyNormalizer.buildActiveDimensionKey(
                        resultSet.getLong("user_id"),
                        patientProfileId,
                        resultSet.getLong("product_id"),
                        recommendationType,
                        status);
                update.setString(1, key);
                update.setLong(2, resultSet.getLong("recommendation_id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private void ensureNoDuplicateActiveDimensionKeys(Connection connection, String tableName) throws SQLException {
        Set<String> keys = new HashSet<>();
        try (PreparedStatement select = connection.prepareStatement(
                        "SELECT active_dimension_key FROM " + tableName + " WHERE active_dimension_key IS NOT NULL");
                ResultSet resultSet = select.executeQuery()) {
            while (resultSet.next()) {
                String key = resultSet.getString("active_dimension_key");
                if (!keys.add(key)) {
                    throw new FlywayException(
                            "Cannot apply recommendation active_dimension_key uniqueness: duplicate active recommendations already exist. Clean up recommendation data before rollout.");
                }
            }
        }
    }

    private String findExistingTableName(Connection connection, String expectedTableName) throws SQLException {
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

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
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

    private boolean uniqueConstraintExists(Connection connection, String tableName, String constraintName)
            throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, true, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null && constraintName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
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
