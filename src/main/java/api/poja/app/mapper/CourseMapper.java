package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.request.CourseInput;
import api.poja.app.model.Course;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JTeacher;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

  public Course toDomain(JCourse course) {
    return Course.builder()
        .id(course.getId())
        .reference(course.getReference())
        .title(course.getTitle())
        .credits(course.getCredits())
        .parcours(course.getParcours())
        .teacherIds(course.getTeachers().stream().map(JTeacher::getId).toList())
        .build();
  }

  public Course toDomain(CourseInput input) {
    return Course.builder()
        .reference(input.reference())
        .title(input.title())
        .credits(input.credits())
        .parcours(input.parcours())
        .teacherIds(List.of())
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.Course toRest(Course course) {
    return api.poja.app.endpoint.rest.model.response.Course.builder()
        .id(course.id() == null ? null : course.id().toString())
        .reference(course.reference())
        .title(course.title())
        .credits(course.credits())
        .parcours(course.parcours())
        .teacherIds(course.teacherIds().stream().map(UUID::toString).toList())
        .build();
  }
}
