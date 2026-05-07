package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V8__AddSymptomTags extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    boolean symptomTableExists = false;
    try (ResultSet tables =
        context.getConnection().getMetaData().getTables(null, null, null, null)) {
      while (tables.next()) {
        if ("symptom".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
          symptomTableExists = true;
          break;
        }
      }
    }
    if (!symptomTableExists) {
      return;
    }

    try (Statement statement = context.getConnection().createStatement()) {
      statement.execute(
          """
                    CREATE TABLE IF NOT EXISTS symptom_tag (
                        symptom_id BIGINT NOT NULL,
                        tag VARCHAR(80) NOT NULL,
                        CONSTRAINT fk_symptom_tag_symptom
                            FOREIGN KEY (symptom_id) REFERENCES symptom(symptom_id) ON DELETE CASCADE
                    )
                    """);
      statement.execute(
          """
                    CREATE INDEX IF NOT EXISTS idx_symptom_tag_symptom_id
                        ON symptom_tag(symptom_id)
                    """);
    }
  }
}
