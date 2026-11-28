package team.ft.reponote.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import team.ft.reponote.common.json.Json;

@org.springframework.stereotype.Component
public class MarkdownRenderer {
  private final Json json;

  public MarkdownRenderer(Json json) {
    this.json = json;
  }

  public String render(
      String projectName, Map<String, Object> row, List<Map<String, Object>> savedWritings) {
    var payload = json.read(row.get("payload").toString());
    StringBuilder md = new StringBuilder("# ").append(projectName).append("\n\n");
    md.append("분석 버전 ")
        .append(row.get("id"))
        .append("\n\n기준 커밋 ")
        .append(row.get("commit_sha"))
        .append("\n\n");
    md.append("## 프로젝트 소개\n\n")
        .append(payload.path("overview").path("purpose").asText())
        .append("\n\n")
        .append(payload.path("overview").path("description").asText())
        .append("\n\n");
    md.append("## 주요 기능\n\n");
    for (var feature : payload.path("features"))
      md.append("### ")
          .append(feature.path("title").asText())
          .append("\n\n")
          .append(feature.path("description").asText())
          .append("\n\n")
          .append(feature.path("concept").asText())
          .append("\n\n");
    md.append("## 사용 언어\n\n");
    json.read(row.get("languages").toString())
        .fields()
        .forEachRemaining(
            e ->
                md.append("- ")
                    .append(e.getKey())
                    .append(" · ")
                    .append(e.getValue().asLong())
                    .append(" bytes\n"));
    md.append("\n## 아키텍처\n\n");
    var nodes = payload.path("architecture").path("nodes");
    Map<String, String> ids = new HashMap<>();
    for (var node : nodes) ids.put(node.path("id").asText(), "n" + ids.size());
    md.append("```mermaid\nflowchart TD\n");
    for (var node : nodes)
      md.append("  ")
          .append(ids.get(node.path("id").asText()))
          .append("[\"")
          .append(mermaid(node.path("title").asText()))
          .append("\"]\n");
    for (var edge : payload.path("architecture").path("edges"))
      md.append("  ")
          .append(ids.get(edge.path("source").asText()))
          .append(" --> ")
          .append(ids.get(edge.path("target").asText()))
          .append("\n");
    md.append("```\n\n");
    for (var node : nodes)
      md.append("- **")
          .append(node.path("title").asText())
          .append("** · ")
          .append(node.path("description").asText())
          .append("\n");
    md.append("\n연결 관계\n\n");
    for (var edge : payload.path("architecture").path("edges"))
      md.append("- ")
          .append(edge.path("source").asText())
          .append(" → ")
          .append(edge.path("target").asText())
          .append(" · ")
          .append(edge.path("label").asText())
          .append("\n");
    Map<String, Map<String, Object>> writings = new HashMap<>();
    savedWritings.forEach(w -> writings.put(w.get("feature_id").toString(), w));
    md.append("\n## 내가 구현한 기능\n\n");
    for (var feature : payload.path("contributions")) {
      var writing = writings.getOrDefault(feature.path("id").asText(), Map.of());
      md.append("### ").append(feature.path("title").asText()).append("\n\n");
      if (writing.get("body") != null) md.append(writing.get("body"));
      else
        md.append(feature.path("summary").asText())
            .append("\n\n")
            .append(feature.path("technical").asText());
      md.append("\n\n근거 커밋 · ").append(feature.path("commitShas")).append("\n\n");
      if (writing.get("note") != null)
        md.append("#### 내 면접 메모\n\n").append(writing.get("note")).append("\n\n");
    }
    md.append("## 주요 개념과 면접 질문\n\n");
    for (var concept : payload.path("concepts"))
      md.append("### ")
          .append(concept.path("name").asText())
          .append("\n\n")
          .append(concept.path("definition").asText())
          .append("\n\n")
          .append(concept.path("application").asText())
          .append("\n\n")
          .append("**")
          .append(concept.path("question").asText())
          .append("**\n\n")
          .append(concept.path("answer").asText())
          .append("\n\n");
    md.append("## 면접 어필 포인트\n\n");
    for (var tip : payload.path("interviewTips"))
      md.append("### ")
          .append(tip.path("title").asText())
          .append("\n\n")
          .append(tip.path("scope").asText().equals("personal") ? "내 구현" : "전체 설계")
          .append("\n\n")
          .append(tip.path("description").asText())
          .append("\n\n");
    Map<String, JsonNode> files = new HashMap<>();
    json.read(row.get("sources").toString()).forEach(f -> files.put(f.path("id").asText(), f));
    md.append("## 근거 코드\n\n");
    for (var range : payload.path("codeExplanations")) {
      var file = files.get(range.path("fileId").asText());
      md.append("### ")
          .append(range.path("title").asText())
          .append("\n\n")
          .append(file.path("path").asText())
          .append(" · ")
          .append(range.path("from").asInt())
          .append("–")
          .append(range.path("to").asInt())
          .append("줄\n\n");
      String[] lines = file.path("code").asText().split("\n", -1);
      String snippet =
          String.join(
              "\n",
              Arrays.copyOfRange(lines, range.path("from").asInt() - 1, range.path("to").asInt()));
      String fence = "`".repeat(Math.max(3, longestBackticks(snippet) + 1));
      md.append(fence)
          .append("\n")
          .append(snippet)
          .append("\n")
          .append(fence)
          .append("\n\n")
          .append(range.path("text").asText())
          .append("\n\n");
    }
    md.append("## 분석 범위\n\n").append(row.get("coverage")).append("\n\n");
    for (var limit : payload.path("limitations"))
      md.append("- ").append(limit.asText()).append("\n");
    return md.toString();
  }

  private String mermaid(String label) {
    return label
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", " ")
        .replace("`", "");
  }

  private int longestBackticks(String value) {
    int max = 0, current = 0;
    for (char c : value.toCharArray()) {
      current = c == '`' ? current + 1 : 0;
      max = Math.max(max, current);
    }
    return max;
  }
}
