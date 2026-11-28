package team.ft.reponote;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LegacySchemaMigrationTest {
  @Test
  void baselinesExistingSchemaAtZeroAndAppliesFirstMigrationWithoutChangingLegacyData()
      throws Exception {
    String url = "jdbc:h2:mem:legacy;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var sql = connection.createStatement()) {
      sql.execute("CREATE TABLE existing_course_data(id INT PRIMARY KEY, label VARCHAR(50))");
      sql.execute("INSERT INTO existing_course_data VALUES(1,'keep this row')");
    }
    var flyway =
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load();
    flyway.migrate();
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var sql = connection.createStatement()) {
      try (var row = sql.executeQuery("SELECT label FROM existing_course_data WHERE id=1")) {
        assertThat(row.next()).isTrue();
        assertThat(row.getString(1)).isEqualTo("keep this row");
      }
      try (var row = sql.executeQuery("SELECT COUNT(*) FROM users")) {
        assertThat(row.next()).isTrue();
        assertThat(row.getInt(1)).isZero();
      }
    }
    assertThat(flyway.info().current().getVersion().toString()).isEqualTo("2");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }

  @Test
  void upgradesV1KeepingExistingCompletedAnalysisAndAddingSectionStorage() throws Exception {
    String url = "jdbc:h2:mem:upgrade;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var sql = connection.createStatement()) {
      sql.execute(
          "INSERT INTO users(id,github_id,login,nickname,access_token)"
              + " VALUES(1,1,'old','old','encrypted')");
      sql.execute(
          "INSERT INTO projects(id,user_id,github_id,owner_login,repo_name,branch_name,repo_type)"
              + " VALUES('p',1,1,'old','repo','main','개인')");
      sql.execute(
          "INSERT INTO"
              + " project_analyses(id,project_id,job_id,commit_sha,schema_version,payload,sources,coverage,languages)"
              + " VALUES('a','p','j','sha','1','{}','[]','{}','{}')");
    }
    var flyway = Flyway.configure().dataSource(url, "sa", "").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var sql = connection.createStatement();
        var row = sql.executeQuery("SELECT complete,payload FROM project_analyses WHERE id='a'")) {
      assertThat(row.next()).isTrue();
      assertThat(row.getBoolean(1)).isTrue();
      assertThat(row.getString(2)).isEqualTo("{}");
    }
  }
}
