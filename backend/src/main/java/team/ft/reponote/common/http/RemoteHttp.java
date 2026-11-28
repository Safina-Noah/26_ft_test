package team.ft.reponote.common.http;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.json.Json;

@Component
public class RemoteHttp {
  private final HttpClient client =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(15))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();
  private final Json json;

  public RemoteHttp(Json json) {
    this.json = json;
  }

  public JsonNode send(HttpRequest request, String provider) {
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        boolean retry = status == 429 || status >= 500;
        String code = provider + "_" + status;
        String message =
            switch (status) {
              case 401, 403 -> provider + " 연결 권한을 확인해주세요.";
              case 429 -> provider + " 사용량 한도에 도달했어요. 잠시 후 다시 시도합니다.";
              default -> provider + " 요청을 처리하지 못했어요. (HTTP " + status + ")";
            };
        throw new Problem(HttpStatus.BAD_GATEWAY, code, message, retry);
      }
      return json.read(response.body());
    } catch (Problem e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new Problem(HttpStatus.BAD_GATEWAY, "INTERRUPTED", "분석이 중단됐어요.", true);
    } catch (java.io.IOException e) {
      throw new Problem(
          HttpStatus.BAD_GATEWAY, provider + "_NETWORK", provider + " 연결이 지연되고 있어요.", true);
    }
  }

  public HttpRequest.Builder request(String url, String token, int seconds) {
    return HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(seconds))
        .header("Authorization", "Bearer " + token)
        .header("Accept", "application/json");
  }
}
