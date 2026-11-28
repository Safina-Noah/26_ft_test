package team.ft.reponote.analysis.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.analysis.model.AnalysisSection;

@Repository
public class AnalysisSectionRepository {
  private final JdbcTemplate db;

  public AnalysisSectionRepository(JdbcTemplate db) {
    this.db = db;
  }

  public List<Map<String, Object>> find(String jobId) {
    return db.queryForList(
        "SELECT section_key,status,attempts,error_message,next_run,payload FROM analysis_sections"
            + " WHERE job_id=?",
        jobId);
  }

  public List<Map<String, Object>> summary(String jobId) {
    var rows = find(jobId);
    var result = new ArrayList<Map<String, Object>>();
    for (var section : AnalysisSection.values())
      for (var row : rows) {
        if (!section.key().equals(row.get("section_key"))) continue;
        var value = new LinkedHashMap<>(row);
        value.remove("payload");
        value.remove("next_run");
        value.put("title", section.title);
        result.add(value);
      }
    return result;
  }

  public void initialize(String jobId) {
    for (var section : AnalysisSection.values())
      db.update(
          "INSERT INTO analysis_sections(job_id,section_key,status) VALUES(?,?,?)",
          jobId,
          section.key(),
          section == AnalysisSection.INTERVIEW ? "BLOCKED" : "PENDING");
  }

  public void begin(String jobId, String key) {
    db.update(
        "UPDATE analysis_sections SET status='RUNNING',attempts=attempts+1,error_message=NULL WHERE"
            + " job_id=? AND section_key=? AND status='PENDING'",
        jobId,
        key);
  }

  public void complete(String jobId, String key, String payload) {
    db.update(
        "UPDATE analysis_sections SET status='COMPLETED',payload=?,error_message=NULL WHERE"
            + " job_id=? AND section_key=?",
        payload,
        jobId,
        key);
  }

  public void failure(String jobId, String key, boolean retry, int attempts, String message) {
    db.update(
        "UPDATE analysis_sections SET status=?,error_message=?,next_run=? WHERE job_id=? AND"
            + " section_key=?",
        retry ? "PENDING" : "FAILED",
        message,
        Timestamp.from(Instant.now().plusSeconds(attempts * 30L)),
        jobId,
        key);
  }

  public void unblockInterview(String jobId) {
    db.update(
        "UPDATE analysis_sections SET status='PENDING' WHERE job_id=? AND section_key='interview'"
            + " AND status='BLOCKED'",
        jobId);
  }

  public void reset(String jobId, String key) {
    db.update(
        "UPDATE analysis_sections SET status='PENDING',attempts=0,error_message=NULL,next_run=?"
            + " WHERE job_id=? AND section_key=?",
        Timestamp.from(Instant.now()),
        jobId,
        key);
  }

  public void failInterrupted(String jobId) {
    db.update(
        "UPDATE analysis_sections SET status='FAILED',error_message='작업이 중단됐어요. 이 항목을 다시 분석해주세요.'"
            + " WHERE job_id=? AND status='RUNNING'",
        jobId);
  }
}
