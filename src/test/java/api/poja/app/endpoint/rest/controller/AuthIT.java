package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.security.JwtTokenProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class AuthIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void disableStreaming() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @Test
  void login_asAdmin_returnsTokenAndRole() {
    var response = login("admin", "admin123");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var role = response.getBody().get("role");
    assertEquals("ADMIN", role);
    var token = (String) response.getBody().get("token");
    assertNotNull(token);
    assertFalse(token.isBlank());
    assertEquals("admin", jwtTokenProvider.getUsername(token));
    assertEquals(List.of("ADMIN"), jwtTokenProvider.getRoles(token));
  }

  @Test
  void login_asTeacher_returnsTokenAndRole() {
    var response = login("teacher", "teacher123");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEACHER", response.getBody().get("role"));
  }

  @Test
  void login_asStudent_returnsTokenAndRole() {
    var response = login("student", "student123");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("STUDENT", response.getBody().get("role"));
  }

  @Test
  void login_wrongPassword_returnsUnauthorized() {
    var response = login("admin", "wrong-password");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void login_unknownUser_returnsUnauthorized() {
    var response = login("ghost", "password");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  private ResponseEntity<Map> login(String username, String password) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        "/auth/login",
        new HttpEntity<>(Map.of("username", username, "password", password), headers),
        Map.class);
  }
}
