package team.ft.reponote.user.controller;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import team.ft.reponote.auth.service.CurrentUser;
import team.ft.reponote.user.dto.ProfileRequest;
import team.ft.reponote.user.dto.UserProfile;
import team.ft.reponote.user.service.UserService;

@RestController
@RequestMapping("/api/me")
public class UserController {
  private final UserService users;
  private final CurrentUser current;

  public UserController(UserService users, CurrentUser current) {
    this.users = users;
    this.current = current;
  }

  @PatchMapping
  public UserProfile update(HttpSession session, @Valid @RequestBody ProfileRequest input) {
    return users.update(current.require(session), input.name());
  }
}
