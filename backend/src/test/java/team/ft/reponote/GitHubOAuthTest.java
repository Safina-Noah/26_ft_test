package team.ft.reponote;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.net.http.HttpRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import team.ft.reponote.auth.service.TokenCipher;
import team.ft.reponote.common.http.RemoteHttp;
import team.ft.reponote.common.json.Json;
import team.ft.reponote.github.client.GitHubClient;
import team.ft.reponote.user.repository.UserRepository;

class GitHubOAuthTest {
  @Test
  void loadsAllPagesOfUserRepositoriesWithEncryptedOAuthToken() {
    Json json = new Json(new ObjectMapper());
    TokenCipher cipher = new TokenCipher("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    UserRepository users = mock(UserRepository.class);
    when(users.findById(1)).thenReturn(Map.of("access_token", cipher.encrypt("test-oauth-token")));
    RemoteHttp http = spy(new RemoteHttp(json));
    ArrayNode first = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    for (int i = 0; i < 100; i++) first.addObject().put("id", i);
    ArrayNode second = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    second.addObject().put("id", 101).put("private", true);
    doReturn(first, second).when(http).send(any(HttpRequest.class), eq("GITHUB"));

    var repos = new GitHubClient(http, users, cipher).repositories(1);
    assertThat(repos).hasSize(101);
    assertThat(repos.getLast().path("private").asBoolean()).isTrue();
    var requests = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    verify(http, times(2)).send(requests.capture(), eq("GITHUB"));
    assertThat(requests.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.uri().getPath()).isEqualTo("/user/repos");
              assertThat(request.uri().getQuery())
                  .contains("affiliation=owner,collaborator,organization_member");
              assertThat(request.headers().firstValue("Authorization"))
                  .contains("Bearer test-oauth-token");
              assertThat(request.method()).isEqualTo("GET");
            });
    assertThat(requests.getAllValues().getLast().uri().getQuery()).contains("page=2");
  }
}
