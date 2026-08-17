package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.mapper.GradeHistoryMapper;
import api.poja.app.mapper.GradeMapper;
import api.poja.app.model.Grade;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.JGradeHistoryRepository;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.JTeacherRepository;
import api.poja.app.repository.model.JGrade;
import api.poja.app.repository.model.JGradeHistory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class GradesService {

  private final JStudentRepository studentRepository;
  private final JExamRepository examRepository;
  private final JCourseRepository courseRepository;
  private final JTeacherRepository teacherRepository;
  private final JGradeRepository gradeRepository;
  private final JGradeHistoryRepository gradeHistoryRepository;
  private final GradeMapper gradeMapper;
  private final GradeHistoryMapper gradeHistoryMapper;

  @Transactional
  public api.poja.app.endpoint.rest.model.response.Grade recordGrade(
      UUID courseId, UUID examId, Grade input, Authentication authentication) {
    var exam =
        examRepository
            .findById(examId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found"));
    if (!exam.getCourse().getId().equals(courseId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Exam not found in this course");
    }
    ensureCanModifyCourse(courseId, authentication);
    var student =
        studentRepository
            .findById(input.studentId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    validateValue(input.value());
    gradeRepository
        .findByStudentIdAndExamId(input.studentId(), examId)
        .forEach(
            previous -> {
              if (Boolean.TRUE.equals(previous.getCurrent())) {
                previous.setCurrent(false);
                gradeRepository.save(previous);
              }
            });
    var saved =
        gradeRepository.save(
            JGrade.builder()
                .student(student)
                .exam(exam)
                .value(input.value())
                .current(true)
                .build());
    saveHistory(saved, input.value(), "Initial grade", authentication.getName());
    return toRestGrade(saved, courseId);
  }

  @Transactional(readOnly = true)
  public List<api.poja.app.endpoint.rest.model.response.Grade> getStudentGrades(
      UUID studentId, UUID courseId, Authentication authentication) {
    ensureCanReadStudent(studentId, authentication);
    return gradeRepository.findByStudentIdAndCurrentTrue(studentId).stream()
        .filter(grade -> courseId == null || grade.getExam().getCourse().getId().equals(courseId))
        .map(grade -> toRestGrade(grade, grade.getExam().getCourse().getId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<api.poja.app.endpoint.rest.model.response.GradeHistoryEntry> getGradeHistory(
      UUID studentId, UUID gradeId, Authentication authentication) {
    ensureCanReadStudent(studentId, authentication);
    studentRepository
        .findById(studentId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    var grade =
        gradeRepository
            .findById(gradeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grade not found"));
    if (!grade.getStudent().getId().equals(studentId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Grade not found for this student");
    }
    return gradeHistoryRepository.findByGradeIdOrderByModifiedAtDesc(gradeId).stream()
        .map(history -> gradeHistoryMapper.toRest(gradeHistoryMapper.toDomain(history)))
        .toList();
  }

  @Transactional
  public api.poja.app.endpoint.rest.model.response.Grade modifyGrade(
      UUID gradeId, Double newValue, String reason, Authentication authentication) {
    var grade =
        gradeRepository
            .findById(gradeId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grade not found"));
    var courseId = grade.getExam().getCourse().getId();
    ensureCanModifyCourse(courseId, authentication);
    if (reason == null || reason.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Reason is required");
    }
    validateValue(newValue);
    grade.setValue(newValue);
    gradeRepository.save(grade);
    saveHistory(grade, newValue, reason, authentication.getName());
    return toRestGrade(grade, courseId);
  }

  private void saveHistory(JGrade grade, Double value, String reason, String modifiedBy) {
    gradeHistoryRepository.save(
        JGradeHistory.builder()
            .grade(grade)
            .value(value)
            .reason(reason)
            .modifiedBy(modifiedBy)
            .modifiedAt(Instant.now())
            .build());
  }

  private void validateValue(Double value) {
    if (value == null || value < 0 || value > 20) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Grade must be between 0 and 20");
    }
  }

  private void ensureCanModifyCourse(UUID courseId, Authentication authentication) {
    var authorities =
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    if (authorities.contains("ROLE_ADMIN")) {
      return;
    }
    if (!authorities.contains("ROLE_TEACHER")) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
    }
    var teacher =
        teacherRepository
            .findByUsername(authentication.getName())
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Access denied"));
    var course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    if (!course.getTeachers().contains(teacher)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "You may only grade your own courses");
    }
  }

  private void ensureCanReadStudent(UUID studentId, Authentication authentication) {
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

  private api.poja.app.endpoint.rest.model.response.Grade toRestGrade(JGrade grade, UUID courseId) {
    return gradeMapper.toRest(gradeMapper.toDomain(grade), courseId);
  }
}
