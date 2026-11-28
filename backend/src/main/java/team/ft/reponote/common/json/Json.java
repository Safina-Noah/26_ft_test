package team.ft.reponote.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class Json {
  final ObjectMapper mapper;

  public Json(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public JsonNode read(String value) {
    try {
      return mapper.readTree(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("JSON을 읽을 수 없습니다.", e);
    }
  }

  public String write(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("JSON을 만들 수 없습니다.", e);
    }
  }

  public <T> T convert(JsonNode node, Class<T> type) {
    try {
      return mapper.treeToValue(node, type);
    } catch (Exception error) {
      throw new IllegalArgumentException("저장된 분석 자료를 읽을 수 없습니다.", error);
    }
  }

  public ObjectNode object() {
    return mapper.createObjectNode();
  }

  public JsonNode tree(Object value) {
    return mapper.valueToTree(value);
  }
}
