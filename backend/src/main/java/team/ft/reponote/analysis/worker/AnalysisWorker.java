package team.ft.reponote.analysis.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.ft.reponote.analysis.client.AiAnalysis;
import team.ft.reponote.analysis.service.*;
import team.ft.reponote.github.service.RepositoryCollector;
import team.ft.reponote.project.service.ProjectService;

@Component
public class AnalysisWorker {
  private final AnalysisJobService jobs;
  private final AnalysisSectionService sections;
  private final ProjectService projects;
  private final RepositoryCollector collector;
  private final AiAnalysis ai;

  public AnalysisWorker(
      AnalysisJobService jobs,
      AnalysisSectionService sections,
      ProjectService projects,
      RepositoryCollector collector,
      AiAnalysis ai) {
    this.jobs = jobs;
    this.sections = sections;
    this.projects = projects;
    this.collector = collector;
    this.ai = ai;
  }

  @Scheduled(fixedDelayString = "${app.analysis.poll-ms:3000}")
  public void tick() {
    var next = jobs.claimNext();
    if (next.isEmpty()) return;
    var job = next.get();
    String id = job.row().get("id").toString(), claim = job.claim();
    try {
      var snapshot = sections.snapshot(job);
      if (snapshot == null) {
        var project = projects.findForAnalysis(job.row().get("project_id").toString());
        long userId = ((Number) project.get("user_id")).longValue();
        String sha =
            job.row().get("sha") == null
                ? collector.resolveSha(userId, project)
                : job.row().get("sha").toString();
        jobs.pinSha(id, claim, sha);
        snapshot = collector.collect(userId, project, sha, stage -> jobs.stage(id, claim, stage));
        jobs.pinSha(id, claim, snapshot.sha());
        sections.initialize(job, snapshot);
      }
      var selected = sections.next(job);
      if (selected.isEmpty()) return;
      var section = selected.get();
      try {
        jobs.stage(id, claim, section.title + " 분석 중");
        var part = ai.analyse(snapshot, section, sections.merged(id));
        sections.complete(job, section, part, snapshot);
      } catch (Exception error) {
        sections.fail(job, section, error);
      }
    } catch (Exception error) {
      jobs.fail(job, error);
    }
  }
}
