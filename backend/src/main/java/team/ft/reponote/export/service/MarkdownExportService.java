package team.ft.reponote.export.service;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.ft.reponote.analysis.service.AnalysisService;
import team.ft.reponote.export.dto.MarkdownDownload;
import team.ft.reponote.project.repository.ProjectRepository;
import team.ft.reponote.writing.repository.FeatureWritingRepository;

@Service
public class MarkdownExportService {
  private final AnalysisService analyses;
  private final ProjectRepository projects;
  private final FeatureWritingRepository writings;
  private final MarkdownRenderer renderer;

  public MarkdownExportService(
      AnalysisService analyses,
      ProjectRepository projects,
      FeatureWritingRepository writings,
      MarkdownRenderer renderer) {
    this.analyses = analyses;
    this.projects = projects;
    this.writings = writings;
    this.renderer = renderer;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public MarkdownDownload download(long userId, String projectId, String version) {
    var project = projects.findOwned(userId, projectId);
    var analysis = analyses.requireRow(userId, projectId, version);
    Object complete = analysis.get("complete");
    if (!(Boolean.TRUE.equals(complete) || Integer.valueOf(1).equals(complete)))
      throw team.ft.reponote.common.error.Problem.invalid("모든 분석 항목이 완료되면 정리본을 다운로드할 수 있어요.");
    String text =
        renderer.render(
            project.get("repo_name").toString(),
            analysis,
            writings.findByAnalysis(analysis.get("id").toString()));
    return new MarkdownDownload(
        project.get("repo_name") + "-overview.md", text.getBytes(StandardCharsets.UTF_8));
  }
}
