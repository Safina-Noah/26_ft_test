package team.ft.reponote;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team.ft.reponote.analysis.repository.AnalysisRepository;
import team.ft.reponote.analysis.service.AnalysisJobService;
import team.ft.reponote.analysis.service.AnalysisService;
import team.ft.reponote.analysis.worker.AnalysisWorker;
import team.ft.reponote.auth.service.TokenCipher;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.json.Json;
import team.ft.reponote.export.service.MarkdownExportService;
import team.ft.reponote.github.client.GitHubClient;
import team.ft.reponote.project.service.ProjectRegistrationService;
import team.ft.reponote.user.repository.UserRepository;
import team.ft.reponote.writing.dto.FeatureWritingRequest;
import team.ft.reponote.writing.repository.FeatureWritingRepository;
import team.ft.reponote.writing.service.FeatureWritingService;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:workspace;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "app.token-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@AutoConfigureMockMvc
class WorkspaceIntegrationTest {
  @Autowired UserRepository users;
  @Autowired ProjectRegistrationService registrations;
  @Autowired AnalysisJobService jobs;
  @Autowired AnalysisService analyses;
  @Autowired AnalysisRepository versions;
  @Autowired FeatureWritingService writingService;
  @Autowired FeatureWritingRepository writings;
  @Autowired JdbcTemplate jdbc;
  @Autowired Json json;
  @Autowired TokenCipher cipher;
  @Autowired MarkdownExportService exports;
  @Autowired MockMvc mvc;
  @Autowired org.springframework.session.SessionRepository sessions;
  @MockitoBean AnalysisWorker worker;
  @MockitoBean GitHubClient github;

  long account() {
    long githubId = Math.abs(UUID.randomUUID().getMostSignificantBits() >> 1);
    return users.login(
        githubId,
        "developer",
        "https://avatars.githubusercontent.com/u/1",
        cipher.encrypt("test-token"),
        null,
        null);
  }

  String project(long userId) {
    return registrations.register(
        userId,
        json.read(
            """
{"id":12,"owner":{"login":"owner","type":"User"},"name":"repo","description":"description","default_branch":"main"}
"""));
  }

  Cookie session(long id) {
    org.springframework.session.Session session =
        (org.springframework.session.Session) sessions.createSession();
    session.setAttribute("userId", id);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "test", "unused", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    session.setAttribute("SPRING_SECURITY_CONTEXT", context);
    sessions.save(session);
    return new Cookie(
        "SESSION",
        Base64.getEncoder()
            .encodeToString(session.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  @Test
  void unauthenticatedAccessAndCrossUserAccessAreRejected() throws Exception {
    long owner = account(), other = account();
    String project = project(owner);
    mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/projects/" + project + "/overview")
                .with(user("other"))
                .cookie(session(other)))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/projects/" + project + "/analyses")
                .with(user("owner"))
                .cookie(session(owner)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/projects/" + project + "/analyses")
                .with(user("owner"))
                .with(csrf())
                .cookie(session(owner)))
        .andExpect(status().isAccepted());
  }

  @Test
  void concurrentStartsReuseOneActiveJob() throws Exception {
    long owner = account();
    String project = project(owner);
    try (var executor = Executors.newFixedThreadPool(4)) {
      List<Future<String>> results = new ArrayList<>();
      for (int i = 0; i < 8; i++) results.add(executor.submit(() -> jobs.start(owner, project)));
      Set<String> ids = new HashSet<>();
      for (var result : results) ids.add(result.get(10, TimeUnit.SECONDS));
      assertThat(ids).hasSize(1);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM analysis_jobs WHERE active_project=?",
                Integer.class,
                project))
        .isEqualTo(1);
  }

  String publish(long owner, String project, String text) {
    String job = jobs.start(owner, project);
    jdbc.update("UPDATE analysis_jobs SET status='RUNNING',claim_token='claim' WHERE id=?", job);
    JsonNode payload =
        json.read(
            """
{"schemaVersion":"1","overview":{"purpose":"복습","description":"프로젝트 설명"},
"features":[],"architecture":{"nodes":[],"edges":[]},"contributions":[
  {"id":"feature","title":"구현 기능","summary":"요약","technical":"기술 설명","commitShas":["abc"]}],
"concepts":[],"interviewTips":[],"codeExplanations":[],"limitations":[]}
""");
    analyses.publish(
        Map.of("id", job, "project_id", project),
        "claim",
        "abc",
        payload,
        List.of(),
        Map.of(),
        Map.of("Java", 10));
    return versions.findLatest(project).orElseThrow().get("id").toString();
  }

  @Test
  void reanalysisPreservesOriginalWritingsAndRejectsStaleEdits() {
    long owner = account();
    String project = project(owner);
    String first = publish(owner, project, "first");
    writingService.save(
        owner, project, "feature", "body", new FeatureWritingRequest(first, "직접 수정한 분석", 0));
    writingService.save(
        owner, project, "feature", "note", new FeatureWritingRequest(first, "나의 면접 메모", 1));
    String next = publish(owner, project, "next");
    assertThat(writings.findByAnalysis(next)).isEmpty();
    assertThat(
            new String(
                exports.download(owner, project, first).bytes(),
                java.nio.charset.StandardCharsets.UTF_8))
        .contains("직접 수정한 분석", "나의 면접 메모");
    assertThatThrownBy(
            () ->
                writingService.save(
                    owner, project, "feature", "body", new FeatureWritingRequest(first, "덮어쓰기", 0)))
        .isInstanceOf(Problem.class)
        .hasMessageContaining("다른 화면");
    assertThat(writings.findByAnalysis(first).getFirst().get("body")).isEqualTo("직접 수정한 분석");
  }

  @Test
  void lostWorkerCannotPublishAndTokensAreEncrypted() {
    long owner = account();
    String project = project(owner);
    String id = jobs.start(owner, project);
    jdbc.update("UPDATE analysis_jobs SET status='RUNNING',claim_token='new-owner' WHERE id=?", id);
    analyses.publish(
        Map.of("id", id, "project_id", project),
        "old-owner",
        "abc",
        json.object(),
        List.of(),
        Map.of(),
        Map.of());
    assertThat(versions.findLatest(project)).isEmpty();
    String encrypted = users.findById(owner).get("access_token").toString();
    assertThat(encrypted).doesNotContain("test-token");
    assertThat(cipher.decrypt(encrypted)).isEqualTo("test-token");
    assertThat(cipher.encrypt("test-token")).isNotEqualTo(encrypted);
  }

  @Test
  void uploadedImagesAreStoredWithMetadataAndRequireOwnership() throws Exception {
    long owner = account(), other = account();
    byte[] bytes =
        Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
    var file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "profile.gif", "image/gif", bytes);
    var response =
        mvc.perform(multipart("/api/images").file(file).cookie(session(owner)).with(csrf()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    String id = json.read(response.getContentAsString()).path("id").asText();
    mvc.perform(get("/api/images/" + id).cookie(session(owner)))
        .andExpect(status().isOk())
        .andExpect(content().bytes(bytes));
    mvc.perform(get("/api/images/" + id).cookie(session(other))).andExpect(status().isNotFound());
    assertThat(jdbc.queryForObject("SELECT width_px FROM images WHERE id=?", Integer.class, id))
        .isEqualTo(1);
  }

  @Autowired team.ft.reponote.analysis.service.AnalysisSectionService sectionService;
  @Autowired team.ft.reponote.analysis.repository.AnalysisSectionRepository sectionRepository;
  @Autowired team.ft.reponote.project.service.ProjectService projectService;
  @MockitoBean team.ft.reponote.analysis.client.AiAnalysis ai;
  @MockitoBean team.ft.reponote.github.service.RepositoryCollector collector;

  private JsonNode sectionPart(team.ft.reponote.analysis.model.AnalysisSection section)
      throws Exception {
    var fixture = new AnalysisContractTest().payload();
    var smallSchema = new team.ft.reponote.analysis.service.AnalysisPayloads(json).schema(section);
    var result = json.object();
    smallSchema
        .path("properties")
        .fieldNames()
        .forEachRemaining(field -> result.set(field, fixture.path(field)));
    return result;
  }

  @Test
  void savesSectionsIndependentlyAndRetriesOnlyFailedItemOnTheSameSnapshot() throws Exception {
    var owner = account();
    var other = account();
    var project = project(owner);
    var jobId = jobs.start(owner, project);
    var snapshot = new AnalysisContractTest().snapshot();
    org.mockito.Mockito.when(
            collector.resolveSha(
                org.mockito.ArgumentMatchers.eq(owner), org.mockito.ArgumentMatchers.anyMap()))
        .thenReturn("abc");
    org.mockito.Mockito.when(
            collector.collect(
                org.mockito.ArgumentMatchers.eq(owner),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.eq("abc"),
                org.mockito.ArgumentMatchers.any()))
        .thenReturn(snapshot);
    var broken = new java.util.concurrent.atomic.AtomicBoolean(true);
    org.mockito.Mockito.when(
            ai.analyse(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              var section =
                  invocation.getArgument(1, team.ft.reponote.analysis.model.AnalysisSection.class);
              if (section == team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE
                  && broken.getAndSet(false)) {
                var invalid = sectionPart(section);
                ((com.fasterxml.jackson.databind.node.ArrayNode)
                        invalid.path("architecture").path("edges"))
                    .add(
                        json.read(
                            "{\"source\":\"n\",\"target\":\"invented\",\"label\":\"잘못된 연결\"}"));
                return invalid;
              }
              if (section == team.ft.reponote.analysis.model.AnalysisSection.INTERVIEW) {
                JsonNode context = invocation.getArgument(2);
                assertThat(context.path("contributions")).hasSize(1);
                assertThat(context.path("architecture").path("nodes")).hasSize(1);
              }
              return sectionPart(section);
            });
    // Drive the real worker one durable section at a time, without network calls or sleeps.
    var runner = new AnalysisWorker(jobs, sectionService, projectService, collector, ai);
    // Other tests may have queued jobs: isolate queue selection for this test.
    jdbc.update(
        "UPDATE analysis_jobs SET next_run=DATEADD('DAY',1,CURRENT_TIMESTAMP) WHERE status='QUEUED'"
            + " AND id<>?",
        jobId);
    runner.tick();
    var partial = analyses.analysis(owner, project, null);
    assertThat(partial.get("complete")).isEqualTo(false);
    assertThat(((JsonNode) partial.get("payload")).path("features")).hasSize(1);
    assertThatThrownBy(() -> exports.download(owner, project, null))
        .hasMessageContaining("모든 분석 항목");
    runner.tick(); // Architecture fails; overview remains saved.
    runner.tick(); // Contributions complete; interview remains blocked.
    var states = sectionRepository.summary(jobId);
    assertThat(states.stream().filter(r -> "COMPLETED".equals(r.get("status"))).count())
        .isEqualTo(2);
    assertThat(states.stream().filter(r -> "BLOCKED".equals(r.get("status"))).count()).isEqualTo(1);
    assertThat(jobs.get(owner, jobId).get("status")).isEqualTo("FAILED");
    mvc.perform(
            post("/api/analysis-jobs/" + jobId + "/sections/architecture/retry")
                .cookie(session(owner)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/analysis-jobs/" + jobId + "/sections/architecture/retry")
                .cookie(session(other))
                .with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/analysis-jobs/" + jobId + "/sections/architecture/retry")
                .cookie(session(owner))
                .with(csrf()))
        .andExpect(status().isAccepted());
    runner.tick(); // Only architecture is retried.
    runner.tick(); // Interview synthesis uses validated earlier findings.
    assertThat(jobs.get(owner, jobId).get("status")).isEqualTo("COMPLETED");
    var complete = analyses.analysis(owner, project, null);
    assertThat(complete.get("id")).isEqualTo(partial.get("id"));
    assertThat(complete.get("complete")).isEqualTo(true);
    assertThat(versions.findVersions(project)).hasSize(1);
    assertThat(exports.download(owner, project, null).bytes()).isNotEmpty();
    org.mockito.Mockito.verify(collector, org.mockito.Mockito.times(1))
        .collect(
            org.mockito.ArgumentMatchers.eq(owner),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.eq("abc"),
            org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verify(ai, org.mockito.Mockito.times(1))
        .analyse(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(
                team.ft.reponote.analysis.model.AnalysisSection.OVERVIEW),
            org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verify(ai, org.mockito.Mockito.times(2))
        .analyse(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(
                team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void sectionLeaseFencesOldWorkerAndTransientRetriesPreserveOtherItems() throws Exception {
    long owner = account();
    String project = project(owner);
    String id = jobs.start(owner, project);
    jdbc.update(
        "UPDATE analysis_jobs SET next_run=DATEADD('DAY',1,CURRENT_TIMESTAMP) WHERE status='QUEUED'"
            + " AND id<>?",
        id);
    var first = jobs.claimNext().orElseThrow();
    jobs.pinSha(id, first.claim(), "abc");
    var snapshot = new AnalysisContractTest().snapshot();
    sectionService.initialize(first, snapshot);
    assertThat(sectionService.next(first))
        .contains(team.ft.reponote.analysis.model.AnalysisSection.OVERVIEW);
    sectionService.fail(
        first,
        team.ft.reponote.analysis.model.AnalysisSection.OVERVIEW,
        new Problem(
            org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT", "잠시 후 재시도", true));
    var second = jobs.claimNext().orElseThrow();
    assertThat(sectionService.next(second))
        .contains(team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE);
    // Lost lease: the old process may finish its HTTP call, but cannot commit its result.
    jdbc.update(
        "UPDATE analysis_jobs SET lease_until=DATEADD('SECOND',-1,CURRENT_TIMESTAMP) WHERE id=?",
        id);
    assertThat(jobs.claimNext()).isEmpty();
    assertThat(jobs.get(owner, id).get("status")).isEqualTo("FAILED");
    assertThatThrownBy(
            () ->
                sectionService.complete(
                    second,
                    team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE,
                    sectionPart(team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE),
                    snapshot))
        .hasMessageContaining("소유권");
    sectionService.retry(owner, id, "architecture");
    var resumed = jobs.claimNext().orElseThrow();
    assertThat(sectionService.snapshot(resumed).sha()).isEqualTo("abc");
    assertThat(sectionService.next(resumed))
        .contains(team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE);
    sectionService.complete(
        resumed,
        team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE,
        sectionPart(team.ft.reponote.analysis.model.AnalysisSection.ARCHITECTURE),
        snapshot);
    var rows = sectionRepository.summary(id);
    assertThat(
            rows.stream()
                .filter(r -> "overview".equals(r.get("section_key")))
                .findFirst()
                .orElseThrow()
                .get("attempts"))
        .isEqualTo(1);
    assertThat(
            rows.stream()
                .filter(r -> "architecture".equals(r.get("section_key")))
                .findFirst()
                .orElseThrow()
                .get("status"))
        .isEqualTo("COMPLETED");
    // Keep this test's unfinished job from competing with other worker integration tests.
    jdbc.update(
        "UPDATE analysis_jobs SET next_run=DATEADD('DAY',1,CURRENT_TIMESTAMP) WHERE id=?", id);
  }
}
