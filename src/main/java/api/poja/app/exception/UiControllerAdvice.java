package api.poja.app.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice(basePackages = "api.poja.app.endpoint.ui")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UiControllerAdvice {

  @ExceptionHandler(ApiException.class)
  public ModelAndView handleApi(ApiException e, HttpServletResponse response) {
    response.setStatus(e.getStatus().value());
    return view(e.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ModelAndView handleNotFound(NoResourceFoundException e, HttpServletResponse response) {
    response.setStatus(HttpStatus.NOT_FOUND.value());
    return view("Page not found");
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView handleUnexpected(Exception e, HttpServletResponse response) {
    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    return view("Unexpected server error");
  }

  private ModelAndView view(String message) {
    var mv = new ModelAndView("ui/error");
    mv.addObject("message", message);
    return mv;
  }
}
