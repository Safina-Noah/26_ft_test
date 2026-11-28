package team.ft.reponote.project.controller;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.auth.service.CurrentUser;
import team.ft.reponote.project.dto.RegisterProjectsRequest;
import team.ft.reponote.project.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
  private final ProjectService projects;
  private final CurrentUser current;

  public ProjectController(ProjectService projects, CurrentUser current) {
    this.projects = projects;
    this.current = current;
  }

  @GetMapping
  public List<Map<String, Object>> list(HttpSession session) {
    return projects.list(current.require(session));
  }

  @PostMapping
  public Map<String, Object> register(
      HttpSession session, @Valid @RequestBody RegisterProjectsRequest input) {
    return projects.register(current.require(session), input);
  }
}
