package team.ft.reponote.image.controller;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.ft.reponote.auth.service.CurrentUser;
import team.ft.reponote.image.dto.ImageUploadResponse;
import team.ft.reponote.image.service.ImageService;

@RestController
@RequestMapping("/api/images")
public class ImageController {
  private final ImageService images;
  private final CurrentUser current;

  public ImageController(ImageService images, CurrentUser current) {
    this.images = images;
    this.current = current;
  }

  @PostMapping
  public ImageUploadResponse upload(
      HttpSession session,
      @RequestParam MultipartFile file,
      @RequestParam(required = false) String projectId)
      throws IOException {
    return images.upload(current.require(session), file, projectId);
  }

  @GetMapping("/{id}")
  public ResponseEntity<byte[]> get(HttpSession session, @PathVariable String id) {
    var image = images.get(current.require(session), id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
        .header("X-Content-Type-Options", "nosniff")
        .body(image.bytes());
  }
}
