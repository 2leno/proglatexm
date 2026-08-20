package api.poja.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

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
                        (request, response, authException) -> response.sendRedirect("/ui/login"))
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            response.sendRedirect("/ui/login?error=csrf")))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/ui/login", "/ui/css/**", "/ui/img/**", "/ui/error")
                    .permitAll()
                    .anyRequest()
                    .hasRole("ADMIN"))
        .addFilterBefore(uiAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
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
                        "/js/**",
                        "/css/**",
                        "/favicon.ico",
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
