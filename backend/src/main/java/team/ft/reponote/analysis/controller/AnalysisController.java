package team.ft.reponote.analysis.controller;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.analysis.service.AnalysisService;
import team.ft.reponote.auth.service.CurrentUser;

@RestController
@RequestMapping("/api/projects")
public class AnalysisController {
  private final AnalysisService analyses;
  private final CurrentUser current;

  public AnalysisController(AnalysisService analyses, CurrentUser current) {
    this.analyses = analyses;
    this.current = current;
  }

  @GetMapping("/{id}/overview")
  public Map<String, Object> overview(
      HttpSession session,
      @PathVariable String id,
      @RequestParam(required = false) String version) {
    return analyses.analysis(current.require(session), id, version);
  }
}
