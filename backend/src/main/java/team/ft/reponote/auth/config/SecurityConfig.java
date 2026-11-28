package team.ft.reponote.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import team.ft.reponote.auth.service.OAuthLoginSuccessHandler;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ClientRegistrationRepository registrations,
      OAuthLoginSuccessHandler loginSuccess,
      @Value("${app.frontend-url}") String frontend)
      throws Exception {
    var resolver =
        new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
    resolver.setAuthorizationRequestCustomizer(
        OAuth2AuthorizationRequestCustomizers.withPkce()
            .andThen(
                builder ->
                    builder.additionalParameters(
                        parameters -> parameters.put("prompt", "select_account"))));
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/health", "/api/csrf", "/api/me", "/error", "/oauth2/**", "/login/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(
            c ->
                c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                    (req, res, err) -> {
                      res.setStatus(401);
                      res.setContentType("application/json");
                      res.getWriter()
                          .write(
                              "{\"message\":\"GitHub 로그인이 필요합니다.\",\"code\":\"UNAUTHENTICATED\"}");
                    }))
        .oauth2Login(
            o ->
                o.authorizationEndpoint(a -> a.authorizationRequestResolver(resolver))
                    .successHandler(loginSuccess)
                    .failureHandler(
                        (request, response, error) ->
                            response.sendRedirect(frontend + "/#/setup?error=github")))
        .logout(
            l ->
                l.logoutUrl("/api/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
    return http.build();
  }
}
