package team.ft.reponote.export.controller;

import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.auth.service.CurrentUser;
import team.ft.reponote.export.service.MarkdownExportService;

@RestController
@RequestMapping("/api/projects")
public class MarkdownExportController {
  private final MarkdownExportService exports;
  private final CurrentUser current;

  public MarkdownExportController(MarkdownExportService exports, CurrentUser current) {
    this.exports = exports;
    this.current = current;
  }

  @GetMapping("/{id}/overview.md")
  public ResponseEntity<byte[]> markdown(
      HttpSession session,
      @PathVariable String id,
      @RequestParam(required = false) String version) {
    var file = exports.download(current.require(session), id, version);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .body(file.bytes());
  }
}
