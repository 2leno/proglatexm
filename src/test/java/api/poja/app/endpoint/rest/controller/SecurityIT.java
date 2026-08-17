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
    var response = exchangeWithBearer("invalid-token");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void authenticatedEndpoint_withValidToken_returnsOk() {
    var token = jwtTokenProvider.generateToken("student", List.of("USER"));
    var response = exchangeWithBearer(token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  private ResponseEntity<String> exchangeWithBearer(String token) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange("/hello", HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }
}
