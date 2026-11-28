package team.ft.reponote.analysis.service;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.ft.reponote.analysis.dto.ClaimedJob;
import team.ft.reponote.analysis.repository.AnalysisJobRepository;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.project.repository.ProjectRepository;

@Service
public class AnalysisJobService {
  private final AnalysisJobRepository jobs;
  private final ProjectRepository projects;
  private final team.ft.reponote.analysis.repository.AnalysisSectionRepository sections;

  public AnalysisJobService(
      AnalysisJobRepository jobs,
      ProjectRepository projects,
      team.ft.reponote.analysis.repository.AnalysisSectionRepository sections) {
    this.jobs = jobs;
    this.projects = projects;
    this.sections = sections;
  }

  @Transactional
  public String start(long userId, String projectId) {
    projects.lockOwned(userId, projectId);
    var active = jobs.findActiveId(projectId);
    if (active.isPresent()) return active.get();
    String id = UUID.randomUUID().toString();
    jobs.insert(id, projectId);
    return id;
  }

  public Map<String, Object> get(long userId, String id) {
    var result = new LinkedHashMap<>(jobs.findOwned(userId, id));
    result.put("sections", sections.summary(id));
    return result;
  }

  public Optional<ClaimedJob> claimNext() {
    Instant now = Instant.now();
    jobs.failExpired(now);
    var next = jobs.findNextQueued(now);
    if (next.isEmpty()) return Optional.empty();
    String claim = UUID.randomUUID().toString();
    if (!jobs.claim(next.get().get("id").toString(), claim, now)) return Optional.empty();
    return Optional.of(new ClaimedJob(next.get(), claim));
  }

  public void stage(String id, String claim, String stage) {
    if (!jobs.updateStage(id, claim, stage)) throw new IllegalStateException("분석 작업 소유권이 변경됐습니다.");
  }

  public void pinSha(String id, String claim, String sha) {
    jobs.pinSha(id, claim, sha);
  }

  public void fail(ClaimedJob job, Exception error) {
    String id = job.row().get("id").toString();
    int attempts = jobs.attempts(id);
    boolean retry = error instanceof Problem p && p.retryable && attempts < 3;
    String code = error instanceof Problem p ? p.code : "ANALYSIS_FAILED";
    String message =
        error instanceof Problem ? error.getMessage() : "분석을 완료하지 못했어요. 서버 설정과 응답 형식을 확인해주세요.";
    jobs.recordFailure(
        id, job.row().get("project_id").toString(), job.claim(), retry, attempts, code, message);
  }
}
