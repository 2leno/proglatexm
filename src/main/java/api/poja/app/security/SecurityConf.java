package api.poja.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConf {

  private final JwtAuthFilter jwtAuthFilter;
  private final UiAuthFilter uiAuthFilter;
  private final ObjectMapper objectMapper;

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Order(1)
  public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/ui/**")
        .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          ((HttpServletResponse) response)
                              .setHeader(
                                  "X-Err",
                                  "AE "
                                      + authException.getClass().getSimpleName()
                                      + " uri="
                                      + request.getRequestURI()
                                      + " dispatch="
                                      + request.getDispatcherType()
                                      + " fwdFrom="
                                      + request.getAttribute("jakarta.servlet.forward.request_uri")
                                      + " viewAttr="
                                      + request.getAttribute(
                                          "org.springframework.web.servlet.View.name")
                                      + " auth="
                                      + SecurityContextHolder.getContext().getAuthentication());
                          response.sendRedirect("/ui/login");
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          ((HttpServletResponse) response)
                              .setHeader(
                                  "X-Err",
                                  "ADE "
                                      + accessDeniedException.getClass().getSimpleName()
                                      + " uri="
                                      + request.getRequestURI()
                                      + " dispatch="
                                      + request.getDispatcherType()
                                      + " auth="
                                      + SecurityContextHolder.getContext().getAuthentication());
                          response.sendRedirect("/ui/login");
                        }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/ui/login", "/ui/css/**", "/ui/error")
                    .permitAll()
                    .anyRequest()
                    .hasRole("ADMIN"))
        .addFilterBefore(debugUiFilter(), CsrfFilter.class)
        .addFilterBefore(uiAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private Filter debugUiFilter() {
    return (request, response, chain) -> {
      if (request instanceof HttpServletRequest httpRequest) {
        var httpResponse = (HttpServletResponse) response;
        if (httpRequest.getRequestURI().equals("/ui/login")
            && !httpResponse.containsHeader("X-Diag")) {
          httpResponse.setHeader(
              "X-Diag",
              "sp="
                  + httpRequest.getServletPath()
                  + " pi="
                  + httpRequest.getPathInfo()
                  + " uri="
                  + httpRequest.getRequestURI()
                  + " ctx="
                  + httpRequest.getContextPath()
                  + " ant="
                  + new AntPathRequestMatcher("/ui/login").matches(httpRequest));
        }
      }
      chain.doFilter(request, response);
    };
  }

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(
                        (request, response, authException) ->
                            writeJson(response, HttpStatus.UNAUTHORIZED, "Authentication required"))
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            writeJson(response, HttpStatus.FORBIDDEN, "Access denied")))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/ping",
                        "/health/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/courses",
                        "/groups",
                        "/students",
                        "/students/*/groups",
                        "/students/*/transcripts/send",
                        "/promotions/*/transcripts/send-all",
                        "/promotions/*/graduates/generate")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/courses/*", "/courses/*/teachers")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/promotions/*/graduates/**", "/promotions")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/groups")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(
                        HttpMethod.POST, "/courses/*/exams", "/courses/*/exams/*/grades")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.PUT, "/grades/*")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.GET, "/students/*/groups/history")
                    .hasAnyRole("ADMIN", "STUDENT")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/students/*/transcripts/*",
                        "/students/*/transcripts/*/status")
                    .hasAnyRole("ADMIN", "STUDENT")
                    .requestMatchers(HttpMethod.POST, "/students/*/transcripts/generate")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .requestMatchers(HttpMethod.GET, "/courses", "/courses/*/exams")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/students/*/grades",
                        "/students/*/grades/*/history",
                        "/students/*/average",
                        "/students/*/average/global")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @SneakyThrows
  private void writeJson(HttpServletResponse response, HttpStatus status, String message) {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
