package team.ft.reponote.writing.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.common.error.Problem;

@Repository
public class FeatureWritingRepository {
  private final JdbcTemplate db;

  public FeatureWritingRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return db.queryForList(sql, args).stream().findFirst().orElseThrow(Problem::missing);
  }

  public List<Map<String, Object>> findByAnalysis(String analysisId) {
    return db.queryForList(
        "SELECT feature_id,body,note,revision FROM feature_writings WHERE analysis_id=?",
        analysisId);
  }

  public List<Map<String, Object>> findNotesForUser(long userId) {
    return db.queryForList(
        """
SELECT w.feature_id,w.note,w.analysis_id,p.id AS project_id,p.repo_name,a.commit_sha
FROM feature_writings w JOIN project_analyses a ON a.id=w.analysis_id
JOIN projects p ON p.id=a.project_id WHERE p.user_id=? AND w.note IS NOT NULL AND w.note<>''
ORDER BY w.updated_at DESC
""",
        userId);
  }

  public long revision(String analysisId, String featureId) {
    return db
        .queryForList(
            "SELECT revision FROM feature_writings WHERE analysis_id=? AND feature_id=?",
            analysisId,
            featureId)
        .stream()
        .findFirst()
        .map(row -> ((Number) row.get("revision")).longValue())
        .orElse(0L);
  }

  public void write(String analysisId, String featureId, String field, String value) {
    if (!Set.of("body", "note").contains(field)) throw Problem.invalid("잘못된 수정 항목입니다.");
    var existing =
        db.queryForList(
            "SELECT feature_id FROM feature_writings WHERE analysis_id=? AND feature_id=?",
            analysisId,
            featureId);
    if (existing.isEmpty())
      db.update(
          "INSERT INTO feature_writings(analysis_id,feature_id) VALUES(?,?)",
          analysisId,
          featureId);
    db.update(
        "UPDATE feature_writings SET "
            + field
            + "=?,revision=revision+1,updated_at=? WHERE analysis_id=? AND feature_id=?",
        value,
        Timestamp.from(Instant.now()),
        analysisId,
        featureId);
  }
}
