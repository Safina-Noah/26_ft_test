package team.ft.reponote.github.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import team.ft.reponote.auth.service.TokenCipher;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.http.RemoteHttp;
import team.ft.reponote.user.repository.UserRepository;

@Service
public class GitHubClient {
  private final RemoteHttp http;
  private final UserRepository users;
  private final TokenCipher cipher;

  public GitHubClient(RemoteHttp http, UserRepository users, TokenCipher cipher) {
    this.http = http;
    this.users = users;
    this.cipher = cipher;
  }

  public static String enc(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  public JsonNode get(long userId, String path) {
    if (!path.startsWith("/")) throw Problem.invalid("잘못된 GitHub 경로");
    String token = cipher.decrypt(users.findById(userId).get("access_token").toString());
    return http.send(
        http.request("https://api.github.com" + path, token, 40)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET()
            .build(),
        "GITHUB");
  }

  public JsonNode repo(long userId, String owner, String name) {
    return get(userId, "/repos/" + enc(owner) + "/" + enc(name));
  }

  public List<JsonNode> repositories(long userId) {
    List<JsonNode> result = new ArrayList<>();
    for (int page = 1; ; page++) {
      JsonNode repos =
          get(
              userId,
              "/user/repos?visibility=all&affiliation=owner,collaborator,organization_member"
                  + "&sort=updated&per_page=100&page="
                  + page);
      repos.forEach(result::add);
      if (repos.size() < 100) break;
    }
    return result;
  }
}
