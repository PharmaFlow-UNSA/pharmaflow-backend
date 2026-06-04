package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V12__NormalizeSeededTherapyReminderProducts extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement statement = context.getConnection().createStatement()) {
      try (ResultSet tables =
          statement.executeQuery(
              """
              SELECT COUNT(*)
              FROM information_schema.tables
              WHERE UPPER(table_name) = 'THERAPY_REMINDER'
              """)) {
        tables.next();
        if (tables.getInt(1) == 0) {
          return;
        }
      }

      statement.executeUpdate(
          """
          UPDATE therapy_reminder
          SET product_id = product_id - 800
          WHERE product_id BETWEEN 801 AND 805
          """);
    }
  }
}
