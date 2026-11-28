package team.ft.reponote.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.ft.reponote.analysis.dto.ClaimedJob;
import team.ft.reponote.analysis.model.AnalysisSection;
import team.ft.reponote.analysis.repository.*;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.json.Json;
import team.ft.reponote.github.dto.RepositorySnapshot;
import team.ft.reponote.project.repository.ProjectRepository;

@Service
public class AnalysisSectionService {
  private final AnalysisSectionRepository sections;
  private final AnalysisJobRepository jobs;
  private final AnalysisRepository analyses;
  private final ProjectRepository projects;
  private final AnalysisPayloads payloads;
  private final AnalysisValidator validator;
  private final Json json;

  public AnalysisSectionService(
      AnalysisSectionRepository sections,
      AnalysisJobRepository jobs,
      AnalysisRepository analyses,
      ProjectRepository projects,
      AnalysisPayloads payloads,
      AnalysisValidator validator,
      Json json) {
    this.sections = sections;
    this.jobs = jobs;
    this.analyses = analyses;
    this.projects = projects;
    this.payloads = payloads;
    this.validator = validator;
    this.json = json;
  }

  private String id(ClaimedJob job) {
    return job.row().get("id").toString();
  }

  private void requireClaim(ClaimedJob job) {
    var row = jobs.lockClaim(id(job));
    if (!"RUNNING".equals(row.get("status")) || !job.claim().equals(row.get("claim_token")))
      throw new IllegalStateException("분석 작업 소유권이 변경됐습니다.");
  }

  public RepositorySnapshot snapshot(ClaimedJob job) {
    String value = jobs.snapshot(id(job));
    return value == null ? null : json.convert(json.read(value), RepositorySnapshot.class);
  }

  @Transactional
  public void initialize(ClaimedJob job, RepositorySnapshot snapshot) {
    requireClaim(job);
    if (jobs.snapshot(id(job)) != null) return;
    jobs.saveSnapshot(id(job), json.write(snapshot));
    sections.initialize(id(job));
    analyses.insert(
        UUID.randomUUID().toString(),
        job.row().get("project_id").toString(),
        id(job),
        snapshot.sha(),
        json.write(payloads.empty()),
        json.write(snapshot.files()),
        json.write(snapshot.coverage()),
        json.write(snapshot.languages()));
    analyses.updateDraft(id(job), json.write(payloads.empty()), false);
  }

  public JsonNode merged(String jobId) {
    Map<AnalysisSection, JsonNode> parts = new EnumMap<>(AnalysisSection.class);
    for (var row : sections.find(jobId))
      if ("COMPLETED".equals(row.get("status")))
        parts.put(
            AnalysisSection.from(row.get("section_key").toString()),
            json.read(row.get("payload").toString()));
    return payloads.merge(parts);
  }

  @Transactional
  public Optional<AnalysisSection> next(ClaimedJob job) {
    requireClaim(job);
    // A process interruption must never leave an item permanently RUNNING.
    sections.failInterrupted(id(job));
    var rows = sections.find(id(job));
    boolean prerequisites =
        rows.stream()
            .filter(r -> !"interview".equals(r.get("section_key")))
            .allMatch(r -> "COMPLETED".equals(r.get("status")));
    if (prerequisites) sections.unblockInterview(id(job));
    rows = sections.find(id(job));
    for (var section : AnalysisSection.values())
      for (var row : rows) {
        if (section.key().equals(row.get("section_key"))
            && "PENDING".equals(row.get("status"))
            && !((Timestamp) row.get("next_run")).toInstant().isAfter(Instant.now())) {
          sections.begin(id(job), section.key());
          return Optional.of(section);
        }
      }
    settle(job);
    return Optional.empty();
  }

  @Transactional
  public void complete(
      ClaimedJob job, AnalysisSection section, JsonNode part, RepositorySnapshot snapshot) {
    requireClaim(job);
    validator.validateSection(section, part, snapshot);
    sections.complete(id(job), section.key(), json.write(part));
    var payload = merged(id(job));
    validator.validate(payload, snapshot);
    analyses.updateDraft(id(job), json.write(payload), false);
    settle(job);
  }

  @Transactional
  public void fail(ClaimedJob job, AnalysisSection section, Exception error) {
    requireClaim(job);
    int attempts =
        ((Number)
                sections.find(id(job)).stream()
                    .filter(r -> section.key().equals(r.get("section_key")))
                    .findFirst()
                    .orElseThrow()
                    .get("attempts"))
            .intValue();
    boolean retry = error instanceof Problem p && p.retryable && attempts < 3;
    String message =
        error instanceof Problem ? error.getMessage() : "응답을 해석하지 못했어요. 이 항목을 다시 분석해주세요.";
    sections.failure(id(job), section.key(), retry, attempts, message);
    settle(job);
  }

  private void settle(ClaimedJob job) {
    var rows = sections.find(id(job));
    boolean prerequisites =
        rows.stream()
            .filter(r -> !"interview".equals(r.get("section_key")))
            .allMatch(r -> "COMPLETED".equals(r.get("status")));
    if (prerequisites) {
      sections.unblockInterview(id(job));
      rows = sections.find(id(job));
    }
    if (rows.stream().allMatch(r -> "COMPLETED".equals(r.get("status")))) {
      analyses.updateDraft(id(job), json.write(merged(id(job))), true);
      jobs.complete(id(job), jobs.lockJob(id(job)).get("sha").toString());
      return;
    }
    var pending =
        rows.stream()
            .filter(r -> "PENDING".equals(r.get("status")))
            .map(r -> ((Timestamp) r.get("next_run")).toInstant())
            .min(Instant::compareTo);
    if (pending.isPresent()) jobs.release(id(job), pending.get());
    else jobs.partialFailure(id(job));
  }

  @Transactional
  public String retry(long userId, String jobId, String key) {
    var owned = jobs.findOwned(userId, jobId);
    String projectId = owned.get("project_id").toString();
    projects.lockOwned(userId, projectId);
    var active = jobs.findActiveId(projectId);
    if (active.isPresent() && !active.get().equals(jobId))
      throw Problem.invalid("이 프로젝트의 다른 분석이 진행 중이에요.");
    jobs.lockJob(jobId);
    AnalysisSection.from(key);
    // An expired worker cannot save after this row lock; fence it before resetting an item.
    var row =
        sections.find(jobId).stream()
            .filter(r -> key.equals(r.get("section_key")))
            .findFirst()
            .orElseThrow(Problem::missing);
    if (!"FAILED".equals(row.get("status"))) throw Problem.invalid("실패한 항목만 다시 분석할 수 있어요.");
    if (active.isPresent()) throw Problem.invalid("진행 중인 항목이 끝난 뒤 다시 시도해주세요.");
    sections.reset(jobId, key);
    jobs.resume(jobId, projectId);
    return jobId;
  }
}
