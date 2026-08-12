package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.endpoint.event.model.UuidCreated;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class HelloWorldController {
  private final EventProducer<UuidCreated> eventProducer;

  @GetMapping("/hello")
  @SneakyThrows
  public String helloWorld() {
    var event = UuidCreated.builder().uuid(UUID.randomUUID().toString()).build();
    eventProducer.accept(List.of(event));
    return "... world!";
  }
}
