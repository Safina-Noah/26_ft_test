package team.ft.reponote.auth.controller;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.auth.dto.AuthStatus;
import team.ft.reponote.auth.service.CurrentUser;
import team.ft.reponote.user.service.UserService;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final UserService users;
  private final CurrentUser current;

  public AuthController(UserService users, CurrentUser current) {
    this.users = users;
    this.current = current;
  }

  @GetMapping("/me")
  public AuthStatus me(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session == null || session.getAttribute("userId") == null)
      return new AuthStatus(false, null);
    return new AuthStatus(true, users.profile(current.require(session)));
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }
}
