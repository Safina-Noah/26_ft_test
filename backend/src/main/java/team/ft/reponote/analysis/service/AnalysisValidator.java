package team.ft.reponote.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import java.util.*;
import org.springframework.stereotype.Component;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.json.Json;
import team.ft.reponote.github.dto.RepositorySnapshot;

@Component
public class AnalysisValidator {
  private final JsonSchema schema;
  private final AnalysisPayloads payloads;

  public AnalysisValidator(Json json) throws java.io.IOException {
    payloads = new AnalysisPayloads(json);
    try (var input = getClass().getResourceAsStream("/analysis.schema.json")) {
      schema =
          JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
              .getSchema(
                  json.read(
                      new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
    }
  }

  public void validateSection(
      team.ft.reponote.analysis.model.AnalysisSection section,
      JsonNode part,
      RepositorySnapshot snapshot) {
    var smallSchema =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(payloads.schema(section));
    if (!smallSchema.validate(part).isEmpty())
      throw Problem.invalid(section.title + " 응답 형식이 올바르지 않습니다.");
    validate(payloads.merge(Map.of(section, part)), snapshot);
  }

  public void validate(JsonNode payload, RepositorySnapshot snapshot) {
    if (!schema.validate(payload).isEmpty())
      throw Problem.invalid("AI 응답이 분석 JSON Schema와 일치하지 않습니다.");
    Set<String> files = new HashSet<>();
    Map<String, Integer> lines = new HashMap<>();
    for (var file : snapshot.files()) {
      files.add(file.id());
      lines.put(file.id(), file.code().split("\n", -1).length);
    }
    Set<String> commits = new HashSet<>();
    for (var commit : snapshot.commits()) commits.add(commit.path("sha").asText());
    Set<String> concepts = new HashSet<>();
    for (JsonNode concept : payload.path("concepts")) {
      if (concept.path("name").asText().isBlank() || !concepts.add(concept.path("name").asText()))
        throw Problem.invalid("개념 이름이 중복됐습니다.");
    }
    for (JsonNode range : payload.path("codeExplanations")) {
      int from = range.path("from").asInt(), to = range.path("to").asInt();
      if (!files.contains(range.path("fileId").asText())
          || from < 1
          || to < from
          || to > lines.get(range.path("fileId").asText())
          || !concepts.contains(range.path("concept").asText()))
        throw Problem.invalid("AI 코드 설명의 파일·줄 범위·개념 참조가 유효하지 않습니다.");
    }
    Set<String> nodeIds = uniqueIds(payload.path("architecture").path("nodes"));
    uniqueIds(payload.path("contributions"));
    uniqueIds(payload.path("features"));
    for (JsonNode edge : payload.path("architecture").path("edges"))
      if (!nodeIds.contains(edge.path("source").asText())
          || !nodeIds.contains(edge.path("target").asText()))
        throw Problem.invalid("아키텍처 연결 대상이 없습니다.");
    for (JsonNode feature : payload.path("contributions")) {
      if (feature.path("commitShas").isEmpty()) throw Problem.invalid("내 기여에 근거 커밋이 필요합니다.");
      for (JsonNode sha : feature.path("commitShas"))
        if (!commits.contains(sha.asText())) throw Problem.invalid("내 기여의 근거 커밋이 수집 기록에 없습니다.");
    }
    checkEvidence(payload, files);
  }

  private Set<String> uniqueIds(JsonNode nodes) {
    Set<String> result = new HashSet<>();
    for (JsonNode node : nodes)
      if (!node.path("id").asText().matches("[a-zA-Z0-9_-]{1,80}")
          || !result.add(node.path("id").asText()))
        throw Problem.invalid("분석 항목 ID가 유효하지 않거나 중복됐습니다.");
    return result;
  }

  private void checkEvidence(JsonNode node, Set<String> files) {
    if (node.isObject()) {
      if (node.has("evidenceIds"))
        for (JsonNode id : node.path("evidenceIds"))
          if (!files.contains(id.asText())) throw Problem.invalid("분석 근거 파일이 없습니다.");
      node.elements().forEachRemaining(child -> checkEvidence(child, files));
    } else if (node.isArray()) node.forEach(child -> checkEvidence(child, files));
  }
}
