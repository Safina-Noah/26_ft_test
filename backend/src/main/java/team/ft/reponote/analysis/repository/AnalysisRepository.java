package team.ft.reponote.analysis.repository;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.common.error.Problem;

@Repository
public class AnalysisRepository {
  private final JdbcTemplate db;

  public AnalysisRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return db.queryForList(sql, args).stream().findFirst().orElseThrow(Problem::missing);
  }

  public Optional<Map<String, Object>> findLatest(String projectId) {
    return db
        .queryForList(
            "SELECT * FROM project_analyses WHERE project_id=? ORDER BY created_at DESC,id DESC"
                + " LIMIT 1",
            projectId)
        .stream()
        .findFirst();
  }

  public Map<String, Object> findInProject(String projectId, String version) {
    return one("SELECT * FROM project_analyses WHERE id=? AND project_id=?", version, projectId);
  }

  public List<Map<String, Object>> findVersions(String projectId) {
    return db.queryForList(
        "SELECT id,commit_sha,created_at,complete FROM project_analyses WHERE project_id=? ORDER BY"
            + " created_at DESC,id DESC",
        projectId);
  }

  public void lockVersion(String id) {
    one("SELECT id FROM project_analyses WHERE id=? FOR UPDATE", id);
  }

  public void insert(
      String id,
      String projectId,
      String jobId,
      String sha,
      String payload,
      String sources,
      String coverage,
      String languages) {
    db.update(
        """
INSERT INTO project_analyses(id,project_id,job_id,commit_sha,schema_version,payload,sources,coverage,languages)
VALUES(?,?,?,?,?,?,?,?,?)
""",
        id,
        projectId,
        jobId,
        sha,
        "1",
        payload,
        sources,
        coverage,
        languages);
  }

  public Optional<Map<String, Object>> findByJob(String jobId) {
    return db.queryForList("SELECT * FROM project_analyses WHERE job_id=?", jobId).stream()
        .findFirst();
  }

  public void updateDraft(String jobId, String payload, boolean complete) {
    db.update(
        "UPDATE project_analyses SET payload=?,complete=? WHERE job_id=?",
        payload,
        complete,
        jobId);
  }
}
