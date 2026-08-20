package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.mapper.CourseMapper;
import api.poja.app.mapper.ExamMapper;
import api.poja.app.model.Course;
import api.poja.app.model.Exam;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.JTeacherRepository;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JExam;
import api.poja.app.repository.model.JTeacher;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class CoursesService {

  private static final double COEFFICIENT_EPSILON = 1e-9;

  private final JCourseRepository courseRepository;
  private final JExamRepository examRepository;
  private final JTeacherRepository teacherRepository;
  private final ExamMapper examMapper;
  private final CourseMapper courseMapper;

  @Transactional(readOnly = true)
  public List<Course> listCourses(Parcours parcours) {
    var courses =
        parcours == null
            ? courseRepository.findAll()
            : courseRepository.findAllByParcours(parcours);
    return courses.stream().map(courseMapper::toDomain).toList();
  }

  @Transactional
  public Course createCourse(Course input) {
    validateCourse(input);
    var course =
        courseRepository.save(
            JCourse.builder()
                .reference(input.reference())
                .title(input.title())
                .credits(input.credits())
                .parcours(input.parcours())
                .build());
    return courseMapper.toDomain(course);
  }

  @Transactional
  public Course updateCourse(UUID courseId, Course input) {
    validateCourse(input);
    var course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    course.setReference(input.reference());
    course.setTitle(input.title());
    course.setCredits(input.credits());
    course.setParcours(input.parcours());
    return courseMapper.toDomain(courseRepository.save(course));
  }

  @Transactional(readOnly = true)
  public List<Exam> getExams(UUID courseId) {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    return examRepository.findByCourseId(courseId).stream().map(examMapper::toDomain).toList();
  }

  @Transactional
  public Course assignTeachers(UUID courseId, List<UUID> teacherIds) {
    var course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    var teachers = new ArrayList<JTeacher>();
    teacherIds.forEach(
        teacherId ->
            teachers.add(
                teacherRepository
                    .findById(teacherId)
                    .orElseThrow(
                        () -> new ApiException(HttpStatus.NOT_FOUND, "Teacher not found"))));
    course.setTeachers(teachers);
    return courseMapper.toDomain(courseRepository.save(course));
  }

  private void validateCourse(Course input) {
    if (input.reference() == null
        || input.reference().isBlank()
        || input.title() == null
        || input.title().isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Reference and title are required");
    }
    if (input.credits() == null || input.credits() <= 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Credits must be positive");
    }
    if (input.parcours() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Parcours is required");
    }
  }

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
