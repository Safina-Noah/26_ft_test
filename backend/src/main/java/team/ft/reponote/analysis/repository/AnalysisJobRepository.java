package team.ft.reponote.analysis.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.common.error.Problem;

@Repository
public class AnalysisJobRepository {
  private final JdbcTemplate db;

  public AnalysisJobRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return db.queryForList(sql, args).stream().findFirst().orElseThrow(Problem::missing);
  }

  public Map<String, Object> findLatest(String projectId) {
    return db
        .queryForList(
            "SELECT id,status,stage,error_code,error_message,attempts,sha FROM analysis_jobs WHERE"
                + " project_id=? ORDER BY created_at DESC,id DESC LIMIT 1",
            projectId)
        .stream()
        .findFirst()
        .orElse(Map.of());
  }

  public Map<String, Object> findOwned(long userId, String jobId) {
    return one(
        """
        SELECT j.id,j.project_id,j.status,j.stage,j.attempts,j.sha,j.error_code,j.error_message
        FROM analysis_jobs j JOIN projects p ON j.project_id=p.id WHERE j.id=? AND p.user_id=?
        """,
        jobId,
        userId);
  }

  public Optional<String> findActiveId(String projectId) {
    return db
        .queryForList("SELECT id FROM analysis_jobs WHERE active_project=?", projectId)
        .stream()
        .findFirst()
        .map(row -> row.get("id").toString());
  }

  public void insert(String id, String projectId) {
    db.update(
        "INSERT INTO analysis_jobs(id,project_id,status,stage,active_project)"
            + " VALUES(?,?,'QUEUED','대기',?)",
        id,
        projectId,
        projectId);
  }

  public void failExpired(Instant now) {
    db.update(
        "UPDATE analysis_sections SET status='FAILED',error_message='작업이 중단됐어요. 이 항목을 다시 분석해주세요.'"
            + " WHERE status='RUNNING' AND job_id IN (SELECT id FROM analysis_jobs WHERE"
            + " status='RUNNING' AND lease_until<?)",
        Timestamp.from(now));
    db.update(
        """
        UPDATE analysis_jobs SET status='FAILED',stage='실패',active_project=NULL,claim_token=NULL,
        error_code='LEASE_EXPIRED',error_message='서버가 작업을 완료하지 못했어요. 다시 분석해주세요.'
        WHERE status='RUNNING' AND lease_until<?
        """,
        Timestamp.from(now));
  }

  public Optional<Map<String, Object>> findNextQueued(Instant now) {
    return db
        .queryForList(
            "SELECT * FROM analysis_jobs WHERE status='QUEUED' AND next_run<=? ORDER BY"
                + " created_at,id LIMIT 1",
            Timestamp.from(now))
        .stream()
        .findFirst();
  }

  public boolean claim(String id, String claim, Instant now) {
    return db.update(
            """
UPDATE analysis_jobs SET status='RUNNING',stage='레포 수집',attempts=attempts+1,claim_token=?,lease_until=?,updated_at=?
WHERE id=? AND status='QUEUED'
""",
            claim,
            Timestamp.from(now.plusSeconds(600)),
            Timestamp.from(now),
            id)
        == 1;
  }

  public boolean updateStage(String id, String claim, String stage) {
    return db.update(
            "UPDATE analysis_jobs SET stage=?,updated_at=?,lease_until=? WHERE id=? AND"
                + " claim_token=? AND status='RUNNING'",
            stage,
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now().plusSeconds(600)),
            id,
            claim)
        == 1;
  }

  public void pinSha(String id, String claim, String sha) {
    db.update("UPDATE analysis_jobs SET sha=? WHERE id=? AND claim_token=?", sha, id, claim);
  }

  public Map<String, Object> lockClaim(String id) {
    return one("SELECT status,claim_token FROM analysis_jobs WHERE id=? FOR UPDATE", id);
  }

  public void complete(String id, String sha) {
    db.update(
        "UPDATE analysis_jobs SET"
            + " status='COMPLETED',stage='완료',active_project=NULL,lease_until=NULL,sha=?,updated_at=?"
            + " WHERE id=?",
        sha,
        Timestamp.from(Instant.now()),
        id);
  }

  public int attempts(String id) {
    return ((Number) one("SELECT attempts FROM analysis_jobs WHERE id=?", id).get("attempts"))
        .intValue();
  }

  public void recordFailure(
      String id,
      String projectId,
      String claim,
      boolean retry,
      int attempts,
      String code,
      String message) {
    db.update(
        """
UPDATE analysis_jobs SET status=?,stage=?,error_code=?,error_message=?,next_run=?,active_project=?,
lease_until=NULL,claim_token=NULL,updated_at=? WHERE id=? AND claim_token=? AND status='RUNNING'
""",
        retry ? "QUEUED" : "FAILED",
        retry ? "재시도 대기" : "실패",
        code,
        message,
        Timestamp.from(Instant.now().plusSeconds(attempts * 30L)),
        retry ? projectId : null,
        Timestamp.from(Instant.now()),
        id,
        claim);
  }

  public Map<String, Object> lockJob(String id) {
    return one("SELECT * FROM analysis_jobs WHERE id=? FOR UPDATE", id);
  }

  public String snapshot(String id) {
    Object value = one("SELECT snapshot FROM analysis_jobs WHERE id=?", id).get("snapshot");
    return value == null ? null : value.toString();
  }

  public void saveSnapshot(String id, String snapshot) {
    db.update("UPDATE analysis_jobs SET snapshot=? WHERE id=?", snapshot, id);
  }

  public void release(String id, Instant next) {
    db.update(
        "UPDATE analysis_jobs SET status='QUEUED',stage='항목별 분석"
            + " 대기',claim_token=NULL,lease_until=NULL,next_run=?,error_code=NULL,error_message=NULL"
            + " WHERE id=?",
        Timestamp.from(next),
        id);
  }

  public void partialFailure(String id) {
    db.update(
        "UPDATE analysis_jobs SET status='FAILED',stage='일부 항목"
            + " 실패',active_project=NULL,claim_token=NULL,lease_until=NULL,error_code='SECTION_FAILED',error_message='완료된"
            + " 항목은 저장됐어요. 실패한 항목만 다시 분석할 수 있어요.' WHERE id=?",
        id);
  }

  public void resume(String id, String projectId) {
    db.update(
        "UPDATE analysis_jobs SET status='QUEUED',stage='항목 재분석"
            + " 대기',active_project=?,next_run=?,error_code=NULL,error_message=NULL WHERE id=?",
        projectId,
        Timestamp.from(Instant.now()),
        id);
  }
}
