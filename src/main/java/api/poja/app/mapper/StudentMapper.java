package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.request.StudentInput;
import api.poja.app.model.Student;
import api.poja.app.repository.model.JStudent;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

  public Student toDomain(JStudent student) {
    return Student.builder()
        .id(student.getId())
        .username(student.getUsername())
        .password(student.getPassword())
        .firstName(student.getFirstName())
        .lastName(student.getLastName())
        .reference(student.getReference())
        .parcours(student.getParcours())
        .email(student.getEmail())
        .build();
  }

  public Student toDomain(StudentInput input) {
    return Student.builder()
        .username(input.username())
        .password(input.password())
        .firstName(input.firstName())
        .lastName(input.lastName())
        .reference(input.reference())
        .parcours(input.parcours())
        .email(input.email())
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.Student toRest(Student student) {
    return api.poja.app.endpoint.rest.model.response.Student.builder()
        .id(student.id() == null ? null : student.id().toString())
        .firstName(student.firstName())
        .lastName(student.lastName())
        .reference(student.reference())
        .parcours(student.parcours())
        .email(student.email())
        .build();
  }
}
