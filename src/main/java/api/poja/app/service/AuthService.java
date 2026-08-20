package api.poja.app.service;

import api.poja.app.endpoint.rest.model.response.LoginResponse;
import api.poja.app.exception.ApiException;
import api.poja.app.model.Role;
import api.poja.app.repository.JAdminRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.JTeacherRepository;
import api.poja.app.security.JwtTokenProvider;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthService {

  private final JAdminRepository adminRepository;
  private final JTeacherRepository teacherRepository;
  private final JStudentRepository studentRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final BCryptPasswordEncoder passwordEncoder;

  public LoginResponse login(String username, String password) {
    if (username == null || password == null) {
      throw invalidCredentials();
    }
    var account = findAccount(username);
    if (account == null || !passwordEncoder.matches(password, account.password())) {
      throw invalidCredentials();
    }
    var token = jwtTokenProvider.generateToken(username, List.of(account.role().name()));
    return LoginResponse.builder().token(token).role(account.role()).build();
  }

  private Account findAccount(String username) {
    var admin = adminRepository.findByUsername(username);
    if (admin.isPresent()) {
      return new Account(admin.get().getPassword(), Role.ADMIN);
    }
    var teacher = teacherRepository.findByUsername(username);
    if (teacher.isPresent()) {
      return new Account(teacher.get().getPassword(), Role.TEACHER);
    }
    return studentRepository
        .findByUsername(username)
        .map(student -> new Account(student.getPassword(), Role.STUDENT))
        .orElse(null);
  }

  private ApiException invalidCredentials() {
    return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
  }

  private record Account(String password, Role role) {}
}
