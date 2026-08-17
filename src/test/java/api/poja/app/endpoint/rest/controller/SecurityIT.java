package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.security.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SecurityIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;

  @Test
  void authenticatedEndpoint_withoutToken_returnsUnauthorized() {
    var response = restTemplate.getForEntity("/hello", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void authenticatedEndpoint_withInvalidToken_returnsUnauthorized() {
    var response = exchangeWithBearer("invalid-token", "/hello");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void authenticatedEndpoint_withValidToken_returnsOk() {
    var token = jwtTokenProvider.generateToken("student", List.of("USER"));
    var response = exchangeWithBearer(token, "/hello");
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void healthEndpoint_withoutToken_returnsOk() {
    var response = restTemplate.getForEntity("/health/up", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void loginEndpoint_withoutToken_returnsNotFound() {
    var response = restTemplate.postForEntity("/auth/login", null, String.class);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createCourse_asTeacher_returnsForbidden() {
    assertEquals(
        HttpStatus.FORBIDDEN, exchangeWithBearer(token("TEACHER"), "/courses", HttpMethod.POST));
  }

  @Test
  void createCourse_asStudent_returnsForbidden() {
    assertEquals(
        HttpStatus.FORBIDDEN, exchangeWithBearer(token("STUDENT"), "/courses", HttpMethod.POST));
  }

  @Test
  void createCourse_asAdmin_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND, exchangeWithBearer(token("ADMIN"), "/courses", HttpMethod.POST));
  }

  @Test
  void listGroups_asTeacher_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND, exchangeWithBearer(token("TEACHER"), "/groups", HttpMethod.GET));
  }

  @Test
  void listGroups_asStudent_returnsForbidden() {
    assertEquals(
        HttpStatus.FORBIDDEN, exchangeWithBearer(token("STUDENT"), "/groups", HttpMethod.GET));
  }

  @Test
  void groupHistory_asStudent_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        exchangeWithBearer(token("STUDENT"), "/students/1/groups/history", HttpMethod.GET));
  }

  @Test
  void groupHistory_asTeacher_returnsForbidden() {
    assertEquals(
        HttpStatus.FORBIDDEN,
        exchangeWithBearer(token("TEACHER"), "/students/1/groups/history", HttpMethod.GET));
  }

  @Test
  void courseExams_asStudent_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        exchangeWithBearer(token("STUDENT"), "/courses/1/exams", HttpMethod.GET));
  }

  @Test
  void transcriptDownload_asTeacher_returnsForbidden() {
    assertEquals(
        HttpStatus.FORBIDDEN,
        exchangeWithBearer(token("TEACHER"), "/students/1/transcripts/2023", HttpMethod.GET));
  }

  @Test
  void transcriptDownload_asStudent_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        exchangeWithBearer(token("STUDENT"), "/students/1/transcripts/2023", HttpMethod.GET));
  }

  @Test
  void transcriptsStatus_asStudent_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        exchangeWithBearer(
            token("STUDENT"), "/students/1/transcripts/2023/status", HttpMethod.GET));
  }

  @Test
  void listPromotions_asTeacher_returnsForbidden() {
    assertEquals(
        HttpStatus.FORBIDDEN, exchangeWithBearer(token("TEACHER"), "/promotions", HttpMethod.GET));
  }

  @Test
  void listPromotions_asAdmin_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND, exchangeWithBearer(token("ADMIN"), "/promotions", HttpMethod.GET));
  }

  @Test
  void graduates_asTeacher_returnsForbidden() {
    assertEquals(
        HttpStatus.FORBIDDEN,
        exchangeWithBearer(token("TEACHER"), "/promotions/1/graduates", HttpMethod.GET));
  }

  @Test
  void graduates_asAdmin_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        exchangeWithBearer(token("ADMIN"), "/promotions/1/graduates/download", HttpMethod.GET));
  }

  @Test
  void courseAverages_asTeacher_returnsNotFound() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        exchangeWithBearer(token("TEACHER"), "/students/1/average/global", HttpMethod.GET));
  }

  private String token(String role) {
    return jwtTokenProvider.generateToken("user", List.of(role));
  }

  private ResponseEntity<String> exchangeWithBearer(String token, String path) {
    return exchangeWithBearer(token, path, HttpMethod.GET);
  }

  private ResponseEntity<String> exchangeWithBearer(String token, String path, HttpMethod method) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
  }
}
