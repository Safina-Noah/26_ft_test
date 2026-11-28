package team.ft.reponote.writing.controller;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.auth.service.CurrentUser;
import team.ft.reponote.writing.dto.FeatureWritingRequest;
import team.ft.reponote.writing.service.FeatureWritingService;

@RestController
@RequestMapping("/api")
public class FeatureWritingController {
  private final FeatureWritingService writings;
  private final CurrentUser current;

  public FeatureWritingController(FeatureWritingService writings, CurrentUser current) {
    this.writings = writings;
    this.current = current;
  }

  @GetMapping("/notes")
  public List<Map<String, Object>> notes(HttpSession session) {
    return writings.notes(current.require(session));
  }

  @PatchMapping("/projects/{id}/features/{feature}")
  public Map<String, Long> edit(
      HttpSession session,
      @PathVariable String id,
      @PathVariable String feature,
      @Valid @RequestBody FeatureWritingRequest input) {
    return Map.of("revision", writings.save(current.require(session), id, feature, "body", input));
  }

  @PutMapping("/projects/{id}/features/{feature}/note")
  public Map<String, Long> note(
      HttpSession session,
      @PathVariable String id,
      @PathVariable String feature,
      @Valid @RequestBody FeatureWritingRequest input) {
    return Map.of("revision", writings.save(current.require(session), id, feature, "note", input));
  }
}
