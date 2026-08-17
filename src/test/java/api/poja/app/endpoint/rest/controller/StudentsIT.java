package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JStudentRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class StudentsIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JStudentRepository studentRepository;

  @BeforeEach
  void disableStreaming() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @Test
  void createStudent_asAdmin_returnsCreatedAndPersists() {
    var body = studentBody("created-student-username", "STD25001", "Alice", "Durand");

    var response = createStudent(token("ADMIN"), body);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Alice", response.getBody().get("firstName"));
    assertEquals("Durand", response.getBody().get("lastName"));
    assertEquals("STD25001", response.getBody().get("reference"));
    assertEquals("EL", response.getBody().get("parcours"));
    assertNotNull(response.getBody().get("id"));
    assertTrue(studentRepository.existsByReference("STD25001"));
  }

  @Test
  void createStudent_duplicateReference_returnsConflict() {
    createStudent(token("ADMIN"), studentBody("dup-ref-user", "STD25002", "Bob", "Martin"));

    var response =
        createStudent(token("ADMIN"), studentBody("other-user", "STD25002", "Bob", "Martin"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void createStudent_duplicateUsername_returnsConflict() {
    createStudent(token("ADMIN"), studentBody("dup-user", "STD25003", "Bob", "Martin"));

    var response =
        createStudent(token("ADMIN"), studentBody("dup-user", "STD25004", "Bob", "Martin"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void createStudent_missingFields_returnsBadRequest() {
    var body = Map.of("firstName", "Alice", "lastName", "Durand", "reference", "STD25005");

    var response = createStudent(token("ADMIN"), body);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createStudent_asTeacher_returnsForbidden() {
    var response =
        createStudent(token("TEACHER"), studentBody("teacher-user", "STD25006", "Alice", "Durand"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void createStudent_withoutToken_returnsUnauthorized() {
    var response = createStudent(null, studentBody("anon-user", "STD25007", "Alice", "Durand"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void createdStudent_canLogin_returnsStudentToken() {
    createStudent(token("ADMIN"), studentBody("created-login-user", "STD25008", "Alice", "Durand"));

    var response = login("created-login-user", "secret-password");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("STUDENT", response.getBody().get("role"));
    var token = (String) response.getBody().get("token");
    assertNotNull(token);
    assertFalse(token.isBlank());
    assertEquals("created-login-user", jwtTokenProvider.getUsername(token));
    assertEquals(List.of("STUDENT"), jwtTokenProvider.getRoles(token));
  }

  private Map<String, Object> studentBody(
      String username, String reference, String firstName, String lastName) {
    return Map.of(
        "firstName", firstName,
        "lastName", lastName,
        "reference", reference,
        "parcours", Parcours.EL.name(),
        "username", username,
        "password", "secret-password");
  }

  private ResponseEntity<Map> createStudent(String token, Object body) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/students", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
  }

  private ResponseEntity<Map> login(String username, String password) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        "/auth/login",
        new HttpEntity<>(Map.of("username", username, "password", password), headers),
        Map.class);
  }

  private String token(String role) {
    return jwtTokenProvider.generateToken("user", List.of(role));
  }
}
