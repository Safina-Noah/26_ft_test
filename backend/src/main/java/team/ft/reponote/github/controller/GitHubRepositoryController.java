package team.ft.reponote.github.controller;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.auth.service.CurrentUser;
import team.ft.reponote.github.dto.RepositoryPage;
import team.ft.reponote.github.service.GitHubRepositoryService;

@RestController
@RequestMapping("/api/github/repositories")
public class GitHubRepositoryController {
  private final GitHubRepositoryService repositories;
  private final CurrentUser current;

  public GitHubRepositoryController(GitHubRepositoryService repositories, CurrentUser current) {
    this.repositories = repositories;
    this.current = current;
  }

  @GetMapping
  public RepositoryPage list(
      HttpSession session,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page) {
    return repositories.search(current.require(session), q, page);
  }
}
