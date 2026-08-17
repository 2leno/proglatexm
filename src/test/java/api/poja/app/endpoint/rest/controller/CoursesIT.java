package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JExam;
import api.poja.app.security.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class CoursesIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JCourseRepository courseRepository;
  @Autowired JExamRepository examRepository;

  @Test
  void createExam_asAdmin_returnsCreatedAndPersists() {
    var course = saveCourse();
    var body = Map.of("name", "Midterm", "schedule", Instant.now().toString(), "coefficient", 0.5);

    var response = postExam(token("ADMIN"), course.getId(), body);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Midterm", response.getBody().get("name"));
    assertEquals(0.5, (Double) response.getBody().get("coefficient"));
    assertNotNull(response.getBody().get("id"));
    var saved = examRepository.findByCourseId(course.getId());
    assertEquals(1, saved.size());
    assertEquals("Midterm", saved.get(0).getName());
  }

  @Test
  void createExam_whenSumExceedsOne_returnsConflict() {
    var course = saveCourse();
    examRepository.save(
        JExam.builder()
            .course(course)
            .name("Midterm")
            .schedule(Instant.now())
            .coefficient(0.6)
            .build());
    var body = Map.of("name", "Final", "schedule", Instant.now().toString(), "coefficient", 0.5);

    var response = postExam(token("ADMIN"), course.getId(), body);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void createExam_onUnknownCourse_returnsNotFound() {
    var body = Map.of("name", "Midterm", "schedule", Instant.now().toString(), "coefficient", 0.5);

    var response = postExam(token("ADMIN"), UUID.randomUUID(), body);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createExam_asTeacher_returnsCreated() {
    var course = saveCourse();
    var body = Map.of("name", "Midterm", "schedule", Instant.now().toString(), "coefficient", 0.5);

    var response = postExam(token("TEACHER"), course.getId(), body);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  @Test
  void createExam_asStudent_returnsForbidden() {
    var course = saveCourse();
    var body = Map.of("name", "Midterm", "schedule", Instant.now().toString(), "coefficient", 0.5);

    var response = postExam(token("STUDENT"), course.getId(), body);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder()
            .reference("C1")
            .title("Mathematics")
            .credits(5)
            .parcours(Parcours.EL)
            .build());
  }

  private ResponseEntity<Map> postExam(String token, UUID courseId, Object body) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/courses/" + courseId + "/exams",
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        Map.class);
  }

  private String token(String role) {
    return jwtTokenProvider.generateToken("user", List.of(role));
  }
}
