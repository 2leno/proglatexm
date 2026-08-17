package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.request.CourseInput;
import api.poja.app.endpoint.rest.model.request.TeacherAssignment;
import api.poja.app.endpoint.rest.model.response.Course;
import api.poja.app.endpoint.rest.model.response.Exam;
import api.poja.app.mapper.CourseMapper;
import api.poja.app.mapper.ExamMapper;
import api.poja.app.model.Parcours;
import api.poja.app.service.CoursesService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CoursesController {

  private final CoursesService coursesService;
  private final CourseMapper courseMapper;
  private final ExamMapper examMapper;

  @GetMapping("/courses")
  public List<Course> listCourses(@RequestParam(required = false) Parcours parcours) {
    return coursesService.listCourses(parcours).stream().map(courseMapper::toRest).toList();
  }

  @PostMapping("/courses")
  @ResponseStatus(HttpStatus.CREATED)
  public Course createCourse(@RequestBody CourseInput input) {
    return courseMapper.toRest(coursesService.createCourse(courseMapper.toDomain(input)));
  }

  @PutMapping("/courses/{courseId}")
  public Course updateCourse(@PathVariable UUID courseId, @RequestBody CourseInput input) {
    return courseMapper.toRest(coursesService.updateCourse(courseId, courseMapper.toDomain(input)));
  }

  @GetMapping("/courses/{courseId}/exams")
  public List<Exam> listExams(@PathVariable UUID courseId) {
    return coursesService.getExams(courseId).stream().map(examMapper::toRest).toList();
  }

  @PutMapping("/courses/{courseId}/teachers")
  public Course assignTeachers(@PathVariable UUID courseId, @RequestBody TeacherAssignment input) {
    return courseMapper.toRest(coursesService.assignTeachers(courseId, input.teacherIds()));
  }
}
