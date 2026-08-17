package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.mapper.StudentMapper;
import api.poja.app.model.Student;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.model.JStudent;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class StudentsService {

  private final JStudentRepository studentRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final StudentMapper studentMapper;

  @Transactional
  public Student createStudent(Student input) {
    validateStudent(input);
    var saved =
        studentRepository.save(
            JStudent.builder()
                .username(input.username())
                .password(passwordEncoder.encode(input.password()))
                .firstName(input.firstName())
                .lastName(input.lastName())
                .reference(input.reference())
                .parcours(input.parcours())
                .build());
    return studentMapper.toDomain(saved);
  }

  private void validateStudent(Student input) {
    if (input.firstName() == null
        || input.firstName().isBlank()
        || input.lastName() == null
        || input.lastName().isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "First name and last name are required");
    }
    if (input.username() == null || input.username().isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Username is required");
    }
    if (input.password() == null || input.password().isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required");
    }
    if (input.reference() == null || input.reference().isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Reference is required");
    }
    if (input.parcours() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Parcours is required");
    }
    if (studentRepository.findByUsername(input.username()).isPresent()) {
      throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
    }
    if (studentRepository.existsByReference(input.reference())) {
      throw new ApiException(HttpStatus.CONFLICT, "Reference already exists");
    }
  }
}
