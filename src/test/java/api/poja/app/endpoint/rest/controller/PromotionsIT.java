package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.repository.JPromotionRepository;
import api.poja.app.repository.model.JPromotion;
import api.poja.app.security.JwtTokenProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PromotionsIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JPromotionRepository promotionRepository;

  @Test
  void listPromotions_returnsSavedPromotions() {
    promotionRepository.save(JPromotion.builder().name("Promotion 2025").year(2025).build());
    promotionRepository.save(JPromotion.builder().name("Promotion 2026").year(2026).build());

    var response = listPromotions(token("ADMIN"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var names = response.getBody().stream().map(promotion -> promotion.get("name")).toList();
    assertTrue(names.contains("Promotion 2025"));
    assertTrue(names.contains("Promotion 2026"));
  }

  @Test
  void listPromotions_asTeacher_returnsForbidden() {
    var response = listPromotionsAsString(token("TEACHER"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void listPromotions_asStudent_returnsForbidden() {
    var response = listPromotionsAsString(token("STUDENT"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void listPromotions_withoutToken_returnsUnauthorized() {
    var response = listPromotionsAsString(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  private ResponseEntity<List<Map<String, Object>>> listPromotions(String token) {
    return restTemplate.exchange(
        "/promotions",
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        new ParameterizedTypeReference<List<Map<String, Object>>>() {});
  }

  private ResponseEntity<String> listPromotionsAsString(String token) {
    return restTemplate.exchange(
        "/promotions", HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
  }

  private HttpHeaders headers(String token) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  private String token(String role) {
    return jwtTokenProvider.generateToken("user", List.of(role));
  }
}
