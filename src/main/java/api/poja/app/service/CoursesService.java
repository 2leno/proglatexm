package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.mapper.ExamMapper;
import api.poja.app.model.Exam;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.model.JExam;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CoursesService {

  private static final double COEFFICIENT_EPSILON = 1e-9;

  private final JCourseRepository courseRepository;
  private final JExamRepository examRepository;
  private final ExamMapper examMapper;

  public Exam createExam(UUID courseId, Exam input) {
    var course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    if (input.coefficient() == null || input.coefficient() <= 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Coefficient must be positive");
    }
    double currentSum =
        examRepository.findByCourseId(courseId).stream().mapToDouble(JExam::getCoefficient).sum();
    if (currentSum + input.coefficient() > 1.0 + COEFFICIENT_EPSILON) {
      throw new ApiException(HttpStatus.CONFLICT, "The sum of coefficients would exceed 1");
    }
    var exam =
        JExam.builder()
            .course(course)
            .name(input.name())
            .schedule(input.schedule())
            .coefficient(input.coefficient())
            .build();
    return examMapper.toDomain(examRepository.save(exam));
  }
}
