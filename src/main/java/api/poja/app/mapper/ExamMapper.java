package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.request.ExamInput;
import api.poja.app.model.Exam;
import api.poja.app.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

  public Exam toDomain(JExam exam) {
    return Exam.builder()
        .id(exam.getId())
        .courseId(exam.getCourse().getId())
        .name(exam.getName())
        .schedule(exam.getSchedule())
        .coefficient(exam.getCoefficient())
        .build();
  }

  public Exam toDomain(ExamInput input) {
    return Exam.builder()
        .name(input.name())
        .schedule(input.schedule())
        .coefficient(input.coefficient())
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.Exam toRest(Exam exam) {
    return api.poja.app.endpoint.rest.model.response.Exam.builder()
        .id(exam.id().toString())
        .name(exam.name())
        .schedule(exam.schedule())
        .coefficient(exam.coefficient())
        .build();
  }
}
