package team.ft.reponote.github.service;

import java.util.Locale;
import org.springframework.stereotype.Service;
import team.ft.reponote.github.client.GitHubClient;
import team.ft.reponote.github.dto.RepositoryPage;

@Service
public class GitHubRepositoryService {
  private final GitHubClient github;

  public GitHubRepositoryService(GitHubClient github) {
    this.github = github;
  }

  public RepositoryPage search(long userId, String query, int page) {
    var repos =
        github.repositories(userId).stream()
            .filter(
                repo ->
                    repo.path("full_name")
                        .asText()
                        .toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)))
            .toList();
    int from = (int) Math.min(Math.max((long) page - 1, 0) * 50, repos.size());
    int to = Math.min(from + 50, repos.size());
    return new RepositoryPage(repos.subList(from, to), to < repos.size());
  }
}
