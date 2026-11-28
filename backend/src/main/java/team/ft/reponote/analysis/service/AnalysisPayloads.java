package team.ft.reponote.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;
import team.ft.reponote.analysis.model.AnalysisSection;
import team.ft.reponote.common.json.Json;

@Component
public class AnalysisPayloads {
  private final Json json;
  private final JsonNode fullSchema;

  public AnalysisPayloads(Json json) throws java.io.IOException {
    this.json = json;
    try (var input = getClass().getResourceAsStream("/analysis.schema.json")) {
      fullSchema = json.read(new String(input.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  public ObjectNode empty() {
    return (ObjectNode)
        json.read(
            """
            {"schemaVersion":"1","overview":{"purpose":"","description":""},"features":[],
             "architecture":{"nodes":[],"edges":[]},"contributions":[],"concepts":[],
             "codeExplanations":[],"interviewTips":[],"limitations":[]}
            """);
  }

  public ObjectNode schema(AnalysisSection section) {
    var result = json.object().put("type", "object").put("additionalProperties", false);
    var properties = result.putObject("properties");
    var required = result.putArray("required");
    var fields = new ArrayList<>(section.fields);
    if (section != AnalysisSection.INTERVIEW)
      fields.addAll(List.of("concepts", "codeExplanations"));
    fields.add("limitations");
    for (String field : fields) {
      properties.set(field, fullSchema.path("properties").path(field));
      required.add(field);
    }
    return result;
  }

  public ObjectNode merge(Map<AnalysisSection, JsonNode> parts) {
    var result = empty();
    Map<String, JsonNode> concepts = new LinkedHashMap<>(), ranges = new LinkedHashMap<>();
    Set<String> limitations = new LinkedHashSet<>();
    for (var section : AnalysisSection.values()) {
      var part = parts.get(section);
      if (part == null) continue;
      section.fields.forEach(field -> result.set(field, part.path(field)));
      part.path("concepts").forEach(c -> concepts.putIfAbsent(c.path("name").asText(), c));
      part.path("codeExplanations")
          .forEach(
              r ->
                  ranges.putIfAbsent(
                      r.path("fileId").asText()
                          + ":"
                          + r.path("from")
                          + ":"
                          + r.path("to")
                          + ":"
                          + r.path("concept").asText(),
                      r));
      part.path("limitations").forEach(l -> limitations.add(l.asText()));
    }
    result.set("concepts", json.tree(concepts.values()));
    result.set("codeExplanations", json.tree(ranges.values()));
    result.set("limitations", json.tree(limitations));
    return result;
  }
}
