package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.request.ExamInput;
import api.poja.app.endpoint.rest.model.response.Exam;
import api.poja.app.mapper.ExamMapper;
import api.poja.app.service.CoursesService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ExamController {

  private final CoursesService coursesService;
  private final ExamMapper examMapper;

  @PostMapping("/courses/{courseId}/exams")
  @ResponseStatus(HttpStatus.CREATED)
  public Exam createExam(@PathVariable UUID courseId, @RequestBody ExamInput input) {
    return examMapper.toRest(coursesService.createExam(courseId, examMapper.toDomain(input)));
  }
}
