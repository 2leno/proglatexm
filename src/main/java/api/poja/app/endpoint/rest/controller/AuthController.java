package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.request.LoginRequest;
import api.poja.app.endpoint.rest.model.response.LoginResponse;
import api.poja.app.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/auth/login")
  public LoginResponse login(@RequestBody(required = false) LoginRequest input) {
    return authService.login(
        input == null ? null : input.username(), input == null ? null : input.password());
  }
}
