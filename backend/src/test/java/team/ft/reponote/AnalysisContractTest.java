package team.ft.reponote;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import team.ft.reponote.analysis.client.AiAnalysis;
import team.ft.reponote.analysis.service.AnalysisValidator;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.http.RemoteHttp;
import team.ft.reponote.common.json.Json;
import team.ft.reponote.github.dto.RepositorySnapshot;
import team.ft.reponote.github.dto.SourceFile;

class AnalysisContractTest {
  final Json json = new Json(new ObjectMapper());

  RepositorySnapshot snapshot() {
    return new RepositorySnapshot(
        "abc",
        List.of(new SourceFile("file-0", "src/Main.java", "one\ntwo", "blob")),
        json.object(),
        List.of(json.read("{\"sha\":\"abc\"}")),
        Map.of());
  }

  JsonNode payload() {
    return json.read(
        """
{"schemaVersion":"1","overview":{"purpose":"이해","description":"설명"},
"features":[{"id":"f","title":"기능","description":"내용","concept":"개념","evidenceIds":["file-0"]}],
"architecture":{"nodes":[{"id":"n","title":"Main","file":"src/Main.java","description":"설명","evidenceIds":["file-0"]}],"edges":[]},
"contributions":[{"id":"mine","category":"서버","title":"구현","summary":"요약","technical":"설명","evidenceIds":["file-0"],"commitShas":["abc"]}],
"concepts":[{"name":"개념","definition":"정의","application":"적용","question":"질문","answer":"답변"}],
"codeExplanations":[{"fileId":"file-0","from":1,"to":2,"title":"동작","text":"해설","concept":"개념"}],
"interviewTips":[],"limitations":[]}
""");
  }

  @Test
  void validatesSchemaAndRejectsInventedReferences() throws Exception {
    var validator = new AnalysisValidator(json);
    validator.validate(payload(), snapshot());
    var invalid = payload().deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("codeExplanations").get(0))
        .put("to", 30);
    assertThatThrownBy(() -> validator.validate(invalid, snapshot())).isInstanceOf(Problem.class);
    var invalidCommit = payload().deepCopy();
    ((com.fasterxml.jackson.databind.node.ArrayNode)
            invalidCommit.path("contributions").get(0).path("commitShas"))
        .set(0, json.tree("invented"));
    assertThatThrownBy(() -> validator.validate(invalidCommit, snapshot()))
        .isInstanceOf(Problem.class);
  }

  JsonNode contributionPart() {
    var part = json.object();
    for (String field : List.of("contributions", "concepts", "codeExplanations", "limitations"))
      part.set(field, payload().path(field));
    return part;
  }

  @Test
  void usesHasaCompatibleChatCompletionsContract() throws Exception {
    var captured = new AtomicReference<JsonNode>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v1/chat/completions",
        exchange -> {
          captured.set(
              json.read(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
          byte[] body =
              json.write(
                      Map.of(
                          "choices",
                          List.of(
                              Map.of(
                                  "finish_reason",
                                  "stop",
                                  "message",
                                  Map.of("content", json.write(contributionPart()))))))
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      var ai =
          new AiAnalysis(
              new RemoteHttp(json),
              json,
              "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
              "local-test-key",
              "qwen3-coder",
              false,
              12000);
      assertThat(
              ai.analyse(
                  snapshot(),
                  team.ft.reponote.analysis.model.AnalysisSection.CONTRIBUTIONS,
                  payload()))
          .isEqualTo(contributionPart());
      assertThat(captured.get().path("model").asText()).isEqualTo("qwen3-coder");
      assertThat(captured.get().has("response_format")).isFalse();
      assertThat(captured.get().path("messages").get(0).path("content").asText())
          .contains("JSON Schema", "contributions")
          .doesNotContain("interviewTips", "architecture");
      new AnalysisValidator(json)
          .validateSection(
              team.ft.reponote.analysis.model.AnalysisSection.CONTRIBUTIONS,
              contributionPart(),
              snapshot());
      assertThatThrownBy(
              () ->
                  new AnalysisValidator(json)
                      .validateSection(
                          team.ft.reponote.analysis.model.AnalysisSection.CONTRIBUTIONS,
                          payload(),
                          snapshot()))
          .isInstanceOf(Problem.class);
    } finally {
      server.stop(0);
    }
  }
}
