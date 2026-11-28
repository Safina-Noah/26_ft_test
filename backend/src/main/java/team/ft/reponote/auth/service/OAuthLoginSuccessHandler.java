package team.ft.reponote.auth.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
  private final AuthService auth;
  private final OAuth2AuthorizedClientService clients;
  private final String frontend;

  public OAuthLoginSuccessHandler(
      AuthService auth,
      OAuth2AuthorizedClientService clients,
      @Value("${app.frontend-url}") String frontend) {
    this.auth = auth;
    this.clients = clients;
    this.frontend = frontend;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    OAuth2User profile = (OAuth2User) authentication.getPrincipal();
    OAuth2AuthorizedClient client =
        clients.loadAuthorizedClient("github", authentication.getName());
    long userId = auth.login(profile, client.getAccessToken().getTokenValue());
    request.getSession().setAttribute("userId", userId);
    clients.removeAuthorizedClient("github", authentication.getName());
    response.sendRedirect(frontend + "/#/home");
  }
}
