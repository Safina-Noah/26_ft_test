package team.ft.reponote.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.ft.reponote.analysis.service.AnalysisJobService;
import team.ft.reponote.project.repository.ProjectRepository;

@Service
public class ProjectRegistrationService {
  private final ProjectRepository projects;
  private final AnalysisJobService jobs;

  public ProjectRegistrationService(ProjectRepository projects, AnalysisJobService jobs) {
    this.projects = projects;
    this.jobs = jobs;
  }

  @Transactional
  public String register(long userId, JsonNode repo) {
    var existing = projects.findIdByGitHubId(userId, repo.path("id").asLong());
    if (existing.isPresent()) return existing.get();
    String id = projects.insert(userId, repo);
    jobs.start(userId, id);
    return id;
  }
}
