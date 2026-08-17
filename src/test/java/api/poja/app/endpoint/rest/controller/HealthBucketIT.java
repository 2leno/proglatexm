package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.app.conf.FacadeIT;
import api.poja.app.conf.FakeBucketComponent;
import api.poja.app.file.bucket.BucketComponent;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

class HealthBucketIT extends FacadeIT {

  @Autowired TestRestTemplate restTemplate;

  @TestConfiguration
  static class TestBeans {

    @Bean
    @Primary
    BucketComponent bucketComponent() {
      return new FakeBucketComponent();
    }
  }

  @Test
  void healthBucket_returnsPresignedUrl() {
    var response = restTemplate.getForEntity("/health/bucket", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var decodedUrl = URLDecoder.decode(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(decodedUrl.startsWith("http://"));
    assertTrue(decodedUrl.contains("/health/"));
  }
}
