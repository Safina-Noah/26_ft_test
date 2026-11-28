package team.ft.reponote.project.repository;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.common.error.Problem;

@Repository
public class ProjectRepository {
  private final JdbcTemplate db;

  public ProjectRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return db.queryForList(sql, args).stream().findFirst().orElseThrow(Problem::missing);
  }

  public Map<String, Object> findOwned(long userId, String id) {
    return one("SELECT * FROM projects WHERE id=? AND user_id=?", id, userId);
  }

  public List<Map<String, Object>> findAllOwned(long userId) {
    return db.queryForList(
        "SELECT * FROM projects WHERE user_id=? ORDER BY created_at DESC,id", userId);
  }

  public String insert(long userId, JsonNode repo) {
    long githubId = repo.path("id").asLong();
    String id = UUID.randomUUID().toString();
    db.update(
        """
INSERT INTO projects(id,user_id,github_id,owner_login,repo_name,description,branch_name,repo_type)
VALUES(?,?,?,?,?,?,?,?)
""",
        id,
        userId,
        githubId,
        repo.path("owner").path("login").asText(),
        repo.path("name").asText(),
        repo.path("description").asText(""),
        repo.path("default_branch").asText(),
        repo.path("owner").path("type").asText().equals("Organization") ? "팀" : "개인");
    return id;
  }

  public Map<String, Object> lockOwned(long userId, String id) {
    return one("SELECT id FROM projects WHERE id=? AND user_id=? FOR UPDATE", id, userId);
  }

  public Map<String, Object> findById(String id) {
    return one("SELECT * FROM projects WHERE id=?", id);
  }

  public Optional<String> findIdByGitHubId(long userId, long githubId) {
    return db
        .queryForList("SELECT id FROM projects WHERE user_id=? AND github_id=?", userId, githubId)
        .stream()
        .findFirst()
        .map(row -> row.get("id").toString());
  }
}
