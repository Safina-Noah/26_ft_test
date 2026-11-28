package team.ft.reponote.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.ft.reponote.analysis.repository.AnalysisJobRepository;
import team.ft.reponote.analysis.repository.AnalysisRepository;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.json.Json;
import team.ft.reponote.project.repository.ProjectRepository;
import team.ft.reponote.writing.repository.FeatureWritingRepository;

@Service
public class AnalysisService {
  private final AnalysisRepository analyses;
  private final AnalysisJobRepository jobs;
  private final ProjectRepository projects;
  private final FeatureWritingRepository writings;
  private final Json json;
  private final team.ft.reponote.analysis.repository.AnalysisSectionRepository sections;

  public AnalysisService(
      AnalysisRepository analyses,
      AnalysisJobRepository jobs,
      ProjectRepository projects,
      FeatureWritingRepository writings,
      Json json,
      team.ft.reponote.analysis.repository.AnalysisSectionRepository sections) {
    this.analyses = analyses;
    this.jobs = jobs;
    this.projects = projects;
    this.writings = writings;
    this.json = json;
    this.sections = sections;
  }

  public Map<String, Object> requireRow(long userId, String projectId, String version) {
    projects.findOwned(userId, projectId);
    if (version == null || version.isBlank())
      return analyses.findLatest(projectId).orElseThrow(Problem::missing);
    return analyses.findInProject(projectId, version);
  }

  public Map<String, Object> analysis(long userId, String projectId, String version) {
    var row = requireRow(userId, projectId, version);
    var result = new LinkedHashMap<String, Object>();
    result.put("id", row.get("id"));
    result.put(
        "complete",
        Boolean.TRUE.equals(row.get("complete")) || Integer.valueOf(1).equals(row.get("complete")));
    result.put("jobId", row.get("job_id"));
    result.put("job", jobs.findOwned(userId, row.get("job_id").toString()));
    result.put("sections", sections.summary(row.get("job_id").toString()));
    result.put("sha", row.get("commit_sha"));
    result.put("createdAt", row.get("created_at"));
    result.put("payload", json.read(row.get("payload").toString()));
    result.put("sources", json.read(row.get("sources").toString()));
    result.put("languages", json.read(row.get("languages").toString()));
    result.put("coverage", json.read(row.get("coverage").toString()));
    result.put("writings", writings.findByAnalysis(row.get("id").toString()));
    result.put("versions", analyses.findVersions(projectId));
    return result;
  }

  @Transactional
  public void publish(
      Map<String, Object> job,
      String claim,
      String sha,
      JsonNode payload,
      Object sources,
      Object coverage,
      Object languages) {
    String id = job.get("id").toString();
    var active = jobs.lockClaim(id);
    if (!"RUNNING".equals(active.get("status")) || !claim.equals(active.get("claim_token"))) return;
    analyses.insert(
        UUID.randomUUID().toString(),
        job.get("project_id").toString(),
        id,
        sha,
        json.write(payload),
        json.write(sources),
        json.write(coverage),
        json.write(languages));
    jobs.complete(id, sha);
  }
}
