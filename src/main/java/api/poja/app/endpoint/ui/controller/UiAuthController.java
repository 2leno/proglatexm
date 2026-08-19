package api.poja.app.endpoint.ui.controller;

import api.poja.app.exception.ApiException;
import api.poja.app.model.Role;
import api.poja.app.security.UiAuthFilter;
import api.poja.app.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class UiAuthController {

  private final AuthService authService;

  @GetMapping("/ui/login")
  public String login() {
    return "ui/login";
  }

  @PostMapping("/ui/login")
  public String login(
      @RequestParam String username,
      @RequestParam String password,
      HttpServletResponse response,
      Model model) {
    try {
      var login = authService.login(username, password);
      if (login.role() != Role.ADMIN) {
        model.addAttribute("error", "Only ADMIN accounts can access this interface");
        return "ui/login";
      }
      var cookie = new Cookie(UiAuthFilter.UI_TOKEN_COOKIE, login.token());
      cookie.setHttpOnly(true);
      cookie.setPath("/ui");
      cookie.setMaxAge(60 * 60);
      response.addCookie(cookie);
      return "redirect:/ui/promotions";
    } catch (ApiException e) {
      model.addAttribute("error", "Invalid credentials");
      return "ui/login";
    }
  }

  @PostMapping("/ui/logout")
  public String logout(HttpServletResponse response) {
    var cookie = new Cookie(UiAuthFilter.UI_TOKEN_COOKIE, "");
    cookie.setHttpOnly(true);
    cookie.setPath("/ui");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
    return "redirect:/ui/login";
  }
}
