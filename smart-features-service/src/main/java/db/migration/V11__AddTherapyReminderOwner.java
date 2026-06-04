package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__AddTherapyReminderOwner extends BaseJavaMigration {

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
      statement.execute(
          """
          ALTER TABLE therapy_reminder
          ADD COLUMN IF NOT EXISTS owner_user_id BIGINT DEFAULT 0 NOT NULL
          """);
    }
  }
}
