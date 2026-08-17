package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.request.GradeInput;
import api.poja.app.exception.ApiException;
import api.poja.app.model.Grade;
import api.poja.app.repository.model.JGrade;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

  public Grade toDomain(GradeInput input, UUID examId) {
    return Grade.builder()
        .studentId(parseUuid(input.studentId()))
        .examId(examId)
        .value(input.value())
        .build();
  }

  public Grade toDomain(JGrade grade) {
    return Grade.builder()
        .id(grade.getId())
        .studentId(grade.getStudent().getId())
        .examId(grade.getExam().getId())
        .value(grade.getValue())
        .current(grade.getCurrent())
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.Grade toRest(Grade grade, UUID courseId) {
    return api.poja.app.endpoint.rest.model.response.Grade.builder()
        .id(grade.id().toString())
        .courseId(courseId.toString())
        .examId(grade.examId().toString())
        .value(grade.value())
        .current(grade.current())
        .build();
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid student id");
    }
  }
}
