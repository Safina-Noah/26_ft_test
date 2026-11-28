package team.ft.reponote.auth.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import team.ft.reponote.common.error.Problem;

@Component
public class CurrentUser {
  public long require(HttpSession session) {
    Object user = session.getAttribute("userId");
    if (!(user instanceof Number))
      throw new Problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "GitHub에 로그인해주세요.", false);
    return ((Number) user).longValue();
  }
}
