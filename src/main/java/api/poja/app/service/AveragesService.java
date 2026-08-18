package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.model.AnnualAverage;
import api.poja.app.model.GlobalAverage;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.model.JGrade;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class AveragesService {

  private final JStudentRepository studentRepository;
  private final JGradeRepository gradeRepository;
  private final StudentAccessGuard accessGuard;

  @Transactional(readOnly = true)
  public AnnualAverage getAnnualAverage(
      UUID studentId, Integer year, Authentication authentication) {
    accessGuard.ensureCanReadStudent(studentId, authentication);
    studentRepository
        .findById(studentId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    if (year == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Year is required");
    }
    var grades =
        gradeRepository.findByStudentIdAndCurrentTrue(studentId).stream()
            .filter(grade -> grade.getExam().getSchedule().atZone(ZoneOffset.UTC).getYear() == year)
            .toList();
    return new AnnualAverage(year, weightedAverage(grades), creditsOf(grades));
  }

  @Transactional(readOnly = true)
  public GlobalAverage getGlobalAverage(UUID studentId, Authentication authentication) {
    accessGuard.ensureCanReadStudent(studentId, authentication);
    studentRepository
        .findById(studentId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    return new GlobalAverage(
        weightedAverage(gradeRepository.findByStudentIdAndCurrentTrue(studentId)));
  }

  private Double weightedAverage(List<JGrade> grades) {
    if (grades.isEmpty()) {
      return 0.0;
    }
    var totalCoefficient =
        grades.stream().mapToDouble(grade -> grade.getExam().getCoefficient()).sum();
    if (totalCoefficient == 0.0) {
      return 0.0;
    }
    var weightedSum =
        grades.stream()
            .mapToDouble(grade -> grade.getValue() * grade.getExam().getCoefficient())
            .sum();
    return weightedSum / totalCoefficient;
  }

  private Integer creditsOf(List<JGrade> grades) {
    return grades.stream()
        .map(grade -> grade.getExam().getCourse().getCredits())
        .distinct()
        .mapToInt(Integer::intValue)
        .sum();
  }
}
