package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JGroupRepository;
import api.poja.app.repository.JPromotionRepository;
import api.poja.app.repository.JStudentGroupPeriodRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.model.JGrade;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class UiBrowseService {

  private final JPromotionRepository promotionRepository;
  private final JGroupRepository groupRepository;
  private final JStudentGroupPeriodRepository periodRepository;
  private final JStudentRepository studentRepository;
  private final JGradeRepository gradeRepository;
  private final GradeAverageComputer gradeAverageComputer;

  @Transactional(readOnly = true)
  public List<PromotionSummary> listPromotions() {
    return promotionRepository.findAll().stream()
        .sorted(Comparator.comparing(promotion -> promotion.getYear(), Comparator.reverseOrder()))
        .map(
            promotion ->
                new PromotionSummary(promotion.getId(), promotion.getName(), promotion.getYear()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PromotionDetail promotionDetail(UUID promotionId) {
    var promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(() -> notFound("Promotion not found"));
    var periods = periodRepository.findByGroupPromotionId(promotionId);
    var groups =
        groupRepository.findByPromotionId(promotionId).stream()
            .sorted(Comparator.comparing(group -> group.getReference()))
            .map(
                group ->
                    new GroupDetail(
                        group.getId(), group.getReference(), studentsOf(group.getId(), periods)))
            .toList();
    return new PromotionDetail(promotion.getId(), promotion.getName(), promotion.getYear(), groups);
  }

  @Transactional(readOnly = true)
  public StudentDetail studentDetail(UUID studentId) {
    var student =
        studentRepository.findById(studentId).orElseThrow(() -> notFound("Student not found"));
    var grades = gradeRepository.findByStudentIdAndCurrentTrue(studentId);
    var gradeViews =
        grades.stream()
            .map(this::toGradeView)
            .sorted(Comparator.comparing(GradeView::schedule))
            .toList();
    var history =
        periodRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
            .map(
                period ->
                    new GroupEntry(
                        period.getGroup().getReference(),
                        period.getStartDate(),
                        period.getEndDate()))
            .toList();
    var annualAverages =
        grades.stream()
            .collect(
                Collectors.groupingBy(
                    grade -> grade.getExam().getSchedule().atZone(ZoneOffset.UTC).getYear()))
            .entrySet()
            .stream()
            .map(
                entry ->
                    new AnnualAverage(
                        entry.getKey(), gradeAverageComputer.weightedAverage(entry.getValue())))
            .sorted(Comparator.comparing(AnnualAverage::year))
            .toList();
    return new StudentDetail(
        toStudentView(student),
        gradeViews,
        gradeAverageComputer.weightedAverage(grades),
        annualAverages,
        history);
  }

  private List<StudentView> studentsOf(
      UUID groupId, List<api.poja.app.repository.model.JStudentGroupPeriod> periods) {
    return periods.stream()
        .filter(period -> period.getGroup().getId().equals(groupId))
        .filter(period -> period.getEndDate() == null)
        .map(period -> toStudentView(period.getStudent()))
        .distinct()
        .toList();
  }

  private StudentView toStudentView(api.poja.app.repository.model.JStudent student) {
    return new StudentView(
        student.getId(),
        student.getReference(),
        student.getFirstName(),
        student.getLastName(),
        student.getParcours().name(),
        student.getEmail());
  }

  private GradeView toGradeView(JGrade grade) {
    return new GradeView(
        grade.getExam().getCourse().getReference(),
        grade.getExam().getCourse().getTitle(),
        grade.getExam().getName(),
        grade.getValue(),
        grade.getExam().getSchedule());
  }

  private ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, message);
  }

  public record PromotionSummary(UUID id, String name, Integer year) {}

  public record PromotionDetail(UUID id, String name, Integer year, List<GroupDetail> groups) {}

  public record GroupDetail(UUID id, String reference, List<StudentView> students) {}

  public record StudentView(
      UUID id,
      String reference,
      String firstName,
      String lastName,
      String parcours,
      String email) {}

  public record StudentDetail(
      StudentView student,
      List<GradeView> grades,
      Double globalAverage,
      List<AnnualAverage> annualAverages,
      List<GroupEntry> history) {}

  public record GradeView(
      String courseReference,
      String courseTitle,
      String examName,
      Double value,
      Instant schedule) {}

  public record GroupEntry(String reference, LocalDate startDate, LocalDate endDate) {}

  public record AnnualAverage(Integer year, Double average) {}
}
