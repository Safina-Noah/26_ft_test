package team.ft.reponote.auth.service;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import team.ft.reponote.user.repository.UserRepository;

@Service
public class AuthService {
  private final UserRepository users;
  private final TokenCipher cipher;

  public AuthService(UserRepository users, TokenCipher cipher) {
    this.users = users;
    this.cipher = cipher;
  }

  public long login(OAuth2User profile, String accessToken) {
    return users.login(
        ((Number) profile.getAttribute("id")).longValue(),
        profile.getAttribute("login"),
        profile.getAttribute("avatar_url"),
        cipher.encrypt(accessToken),
        null,
        null);
  }
}
