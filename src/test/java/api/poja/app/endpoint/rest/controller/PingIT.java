package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.app.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

class PingIT extends FacadeIT {

  @Autowired TestRestTemplate restTemplate;

  @Test
  void ping_returnsPong() {
    var response = restTemplate.getForEntity("/ping", String.class);
    assertEquals("pong", response.getBody());
  }
}
