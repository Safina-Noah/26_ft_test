package team.ft.reponote.project.service;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import team.ft.reponote.analysis.repository.AnalysisJobRepository;
import team.ft.reponote.analysis.repository.AnalysisRepository;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.github.client.GitHubClient;
import team.ft.reponote.image.service.ImageService;
import team.ft.reponote.project.dto.RegisterProjectsRequest;
import team.ft.reponote.project.dto.RepositorySelection;
import team.ft.reponote.project.repository.ProjectRepository;

@Service
public class ProjectService {
  private final ProjectRepository projects;
  private final ProjectRegistrationService registrations;
  private final AnalysisRepository analyses;
  private final AnalysisJobRepository jobs;
  private final ImageService images;
  private final GitHubClient github;
  private final team.ft.reponote.analysis.repository.AnalysisSectionRepository sections;

  public ProjectService(
      ProjectRepository projects,
      ProjectRegistrationService registrations,
      AnalysisRepository analyses,
      AnalysisJobRepository jobs,
      ImageService images,
      GitHubClient github,
      team.ft.reponote.analysis.repository.AnalysisSectionRepository sections) {
    this.projects = projects;
    this.registrations = registrations;
    this.analyses = analyses;
    this.jobs = jobs;
    this.images = images;
    this.github = github;
    this.sections = sections;
  }

  public List<Map<String, Object>> list(long userId) {
    return projects.findAllOwned(userId).stream().map(p -> card(userId, p)).toList();
  }

  public Map<String, Object> findForAnalysis(String id) {
    return projects.findById(id);
  }

  public Map<String, Object> card(long userId, Map<String, Object> p) {
    String id = p.get("id").toString();
    var latest = analyses.findLatest(id);
    var result = new LinkedHashMap<String, Object>();
    result.put("id", id);
    result.put("owner", p.get("owner_login"));
    result.put("title", p.get("repo_name"));
    result.put("description", p.get("description"));
    result.put("type", p.get("repo_type"));
    result.put("full", latest.isPresent());
    result.put("color", "violet");
    result.put("art", "image");
    result.put("tags", List.of());
    var job = new LinkedHashMap<>(jobs.findLatest(id));
    if (job.get("id") != null) job.put("sections", sections.summary(job.get("id").toString()));
    result.put("job", job);
    result.put(
        "complete",
        latest
            .map(
                a ->
                    Boolean.TRUE.equals(a.get("complete"))
                        || Integer.valueOf(1).equals(a.get("complete")))
            .orElse(false));
    result.put("imageUrl", images.latestUrl(userId, id, "project").orElse(null));
    result.put("analysisId", latest.map(a -> a.get("id")).orElse(null));
    return result;
  }

  public Map<String, Object> register(long userId, RegisterProjectsRequest request) {
    Set<Long> accessible = new HashSet<>();
    github.repositories(userId).forEach(r -> accessible.add(r.path("id").asLong()));
    List<String> ids = new ArrayList<>();
    for (RepositorySelection input : request.repositories()) {
      var repo = github.repo(userId, input.owner(), input.name());
      if (!accessible.contains(repo.path("id").asLong()))
        throw new Problem(
            HttpStatus.FORBIDDEN, "REPO_ACCESS", "GitHub 계정으로 접근 가능한 레포를 선택해주세요.", false);
      try {
        ids.add(registrations.register(userId, repo));
      } catch (org.springframework.dao.DuplicateKeyException e) {
        ids.add(
            projects
                .findIdByGitHubId(userId, repo.path("id").asLong())
                .orElseThrow(Problem::missing));
      }
    }
    // Each repository is independently committed. Retrying the same batch reuses successful
    // registrations.
    return Map.of("projectIds", ids);
  }
}
