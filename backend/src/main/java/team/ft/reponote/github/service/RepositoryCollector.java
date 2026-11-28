package team.ft.reponote.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.github.client.GitHubClient;
import team.ft.reponote.github.dto.RepositorySnapshot;
import team.ft.reponote.github.dto.SourceFile;

@Service
public class RepositoryCollector {

  private final GitHubClient github;
  private final int maxFiles, maxChars, maxBytes, maxCommits;

  public RepositoryCollector(
      GitHubClient github,
      @Value("${app.analysis.max-files}") int maxFiles,
      @Value("${app.analysis.max-input-chars}") int maxChars,
      @Value("${app.analysis.max-file-bytes}") int maxBytes,
      @Value("${app.analysis.max-commits}") int maxCommits) {
    this.github = github;
    this.maxFiles = maxFiles;
    this.maxChars = maxChars;
    this.maxBytes = maxBytes;
    this.maxCommits = maxCommits;
  }

  public String resolveSha(long userId, Map<String, Object> project) {
    String root =
        "/repos/"
            + GitHubClient.enc(project.get("owner_login").toString())
            + "/"
            + GitHubClient.enc(project.get("repo_name").toString());
    return github
        .get(userId, root + "/commits/" + GitHubClient.enc(project.get("branch_name").toString()))
        .path("sha")
        .asText();
  }

  public RepositorySnapshot collect(
      long userId, Map<String, Object> project, String sha, Consumer<String> stage) {
    String root =
        "/repos/"
            + GitHubClient.enc(project.get("owner_login").toString())
            + "/"
            + GitHubClient.enc(project.get("repo_name").toString());
    JsonNode tree = github.get(userId, root + "/git/trees/" + sha + "?recursive=1");
    String author = storeLogin(userId);
    JsonNode commits =
        github.get(
            userId,
            root
                + "/commits?sha="
                + sha
                + "&author="
                + GitHubClient.enc(author)
                + "&per_page="
                + Math.min(maxCommits, 100));
    List<JsonNode> details = new ArrayList<>();
    Set<String> changed = new HashSet<>();
    int patchBudget = maxChars / 3;
    int patchChars = 0;
    for (JsonNode commit : commits) {
      stage.accept("기여 커밋 수집");
      JsonNode detail = github.get(userId, root + "/commits/" + commit.path("sha").asText());
      var summary = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
      summary.put("sha", detail.path("sha").asText());
      summary.put(
          "message",
          detail
              .path("commit")
              .path("message")
              .asText()
              .substring(
                  0, Math.min(1000, detail.path("commit").path("message").asText().length())));
      var changedFiles = summary.putArray("files");
      for (JsonNode file : detail.path("files")) {
        String filename = file.path("filename").asText();
        changed.add(filename);
        if (!eligible(filename) || patchChars >= patchBudget) continue;
        var item = changedFiles.addObject();
        item.put("filename", filename);
        String patch = file.path("patch").asText();
        patch =
            patch.substring(0, Math.min(patch.length(), Math.min(3000, patchBudget - patchChars)));
        patch = redact(patch);
        item.put("patch", patch);
        patchChars += patch.length();
      }
      details.add(summary);
    }
    List<JsonNode> candidates = new ArrayList<>();
    for (JsonNode item : tree.path("tree"))
      if (item.path("type").asText().equals("blob") && eligible(item.path("path").asText()))
        candidates.add(item);
    candidates.sort(
        Comparator.<JsonNode>comparingInt(n -> priority(n.path("path").asText(), changed))
            .thenComparing(n -> n.path("path").asText()));
    List<SourceFile> files = new ArrayList<>();
    List<String> omitted = new ArrayList<>();
    int chars = patchChars;
    for (JsonNode item : candidates) {
      String path = item.path("path").asText();
      if (files.size() >= maxFiles || item.path("size").asLong() > maxBytes || chars >= maxChars) {
        omitted.add(path);
        continue;
      }
      stage.accept("코드 수집 · " + (files.size() + 1));
      JsonNode blob = github.get(userId, root + "/git/blobs/" + item.path("sha").asText());
      if (!blob.path("encoding").asText().equals("base64")) {
        omitted.add(path);
        continue;
      }
      byte[] bytes = Base64.getMimeDecoder().decode(blob.path("content").asText());
      if (bytes.length > maxBytes) {
        omitted.add(path);
        continue;
      }
      String code = new String(bytes, StandardCharsets.UTF_8);
      if (code.indexOf('\0') >= 0 || chars + code.length() > maxChars) {
        omitted.add(path);
        continue;
      }
      // Preserve line numbers while masking common inline credential assignments.
      code = redact(code);
      files.add(new SourceFile("file-" + files.size(), path, code, item.path("sha").asText()));
      chars += code.length();
    }
    if (files.isEmpty()) throw Problem.invalid("분석 가능한 소스 파일이 없습니다.");
    Map<String, Object> coverage = new LinkedHashMap<>();
    coverage.put("treeTruncated", tree.path("truncated").asBoolean());
    coverage.put("eligibleFileCount", candidates.size());
    coverage.put("analysedFileCount", files.size());
    coverage.put("omittedPaths", omitted);
    coverage.put("commitLimit", maxCommits);
    coverage.put("commitListMayBeLimited", commits.size() >= maxCommits);
    coverage.put("inputChars", chars);
    coverage.put("commitPatchesLimited", patchChars >= patchBudget);
    List<String> treePaths = new ArrayList<>();
    int pathChars = 0;
    for (JsonNode entry : tree.path("tree")) {
      String path = entry.path("path").asText();
      if (pathChars + path.length() > 10000) break;
      treePaths.add(path);
      pathChars += path.length();
    }
    coverage.put("treePaths", treePaths);
    coverage.put("pathListLimited", treePaths.size() < tree.path("tree").size());
    coverage.put("note", "파일 목록은 기준 커밋, 언어 비율은 수집 시점의 GitHub 기본 브랜치 통계입니다.");
    return new RepositorySnapshot(
        sha, files, github.get(userId, root + "/languages"), details, coverage);
  }

  private String redact(String value) {
    return value.replaceAll(
        "(?im)^.*(?:password|api[_-]?key|client[_-]?secret|access[_-]?token)\\s*[:=].*$",
        "[credential assignment redacted]");
  }

  private String storeLogin(long userId) {
    return github.get(userId, "/user").path("login").asText();
  }

  private boolean eligible(String path) {
    String p = path.toLowerCase(Locale.ROOT);
    if (p.matches(".*(^|/)(node_modules|vendor|build|dist|target|\\.git)(/|$).*")) return false;
    if (p.matches(".*(^|/)(\\.env[^/]*|[^/]*lock[^/]*|[^/]*\\.(pem|key|p12|pfx))$")) return false;
    return p.matches(
            ".*\\.(java|kt|dart|js|jsx|ts|tsx|html|css|scss|sql|md|gradle|yml|yaml|properties|json|xml|py|go|rs)$")
        || p.endsWith("Dockerfile");
  }

  private int priority(String path, Set<String> changed) {
    if (changed.contains(path)) return 0;
    if (path.matches("(?i).*(readme|package.json|build.gradle|pom.xml|pubspec.yaml)$")) return 1;
    if (path.matches("(?i).*(application|controller|service|repository|router|main|app).*"))
      return 2;
    return 3;
  }
}
