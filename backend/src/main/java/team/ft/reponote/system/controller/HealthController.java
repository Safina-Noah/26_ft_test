package team.ft.reponote.system.controller;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.system.dto.HealthResponse;

@RestController
@RequestMapping("/api")
public class HealthController {
  private final String clientId, aiKey, tokenKey;

  public HealthController(
      @Value("${spring.security.oauth2.client.registration.github.client-id}") String clientId,
      @Value("${app.ai.api-key}") String aiKey,
      @Value("${app.token-key}") String tokenKey) {
    this.clientId = clientId;
    this.aiKey = aiKey;
    this.tokenKey = tokenKey;
  }

  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse(
        "UP", !clientId.equals("unconfigured") && !tokenKey.isBlank(), !aiKey.isBlank());
  }
}
