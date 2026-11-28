package team.ft.reponote.analysis.controller;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.analysis.service.AnalysisJobService;
import team.ft.reponote.auth.service.CurrentUser;

@RestController
@RequestMapping("/api")
public class AnalysisJobController {
  private final AnalysisJobService jobs;
  private final CurrentUser current;
  private final team.ft.reponote.analysis.service.AnalysisSectionService sections;

  public AnalysisJobController(
      AnalysisJobService jobs,
      CurrentUser current,
      team.ft.reponote.analysis.service.AnalysisSectionService sections) {
    this.jobs = jobs;
    this.current = current;
    this.sections = sections;
  }

  @PostMapping("/projects/{id}/analyses")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> start(HttpSession session, @PathVariable String id) {
    return Map.of("jobId", jobs.start(current.require(session), id));
  }

  @PostMapping("/analysis-jobs/{id}/sections/{key}/retry")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> retry(
      HttpSession session, @PathVariable String id, @PathVariable String key) {
    return Map.of("jobId", sections.retry(current.require(session), id, key));
  }

  @GetMapping("/analysis-jobs/{id}")
  public Map<String, Object> job(HttpSession session, @PathVariable String id) {
    return jobs.get(current.require(session), id);
  }
}
