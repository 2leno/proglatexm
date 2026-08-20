package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.repository.JStudentRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StudentAccessGuard {

  private final JStudentRepository studentRepository;

  public void ensureCanReadStudent(UUID studentId, Authentication authentication) {
    var authorities =
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    if (authorities.contains("ROLE_STUDENT") && !authorities.contains("ROLE_ADMIN")) {
      var owner =
          studentRepository
              .findByUsername(authentication.getName())
              .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Access denied"));
      if (!owner.getId().equals(studentId)) {
        throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
      }
    }
  }
}
