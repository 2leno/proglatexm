package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.request.StudentInput;
import api.poja.app.endpoint.rest.model.response.Student;
import api.poja.app.mapper.StudentMapper;
import api.poja.app.service.StudentsService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class StudentsController {

  private final StudentsService studentsService;
  private final StudentMapper studentMapper;

  @PostMapping("/students")
  @ResponseStatus(HttpStatus.CREATED)
  public Student createStudent(@RequestBody StudentInput input) {
    return studentMapper.toRest(studentsService.createStudent(studentMapper.toDomain(input)));
  }
}
