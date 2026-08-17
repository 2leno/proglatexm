package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.JTeacherRepository;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JExam;
import api.poja.app.repository.model.JTeacher;
import api.poja.app.security.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class CoursesIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JCourseRepository courseRepository;
  @Autowired JExamRepository examRepository;
  @Autowired JTeacherRepository teacherRepository;

  @BeforeEach
  void disableStreaming() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

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

  @Test
  void createCourse_asAdmin_returnsCreatedAndPersists() {
    var response = createCourse(token("ADMIN"), courseBody("COURSE-1", "Algebra", 4, "EL"));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Algebra", response.getBody().get("title"));
    assertEquals("COURSE-1", response.getBody().get("reference"));
    assertNotNull(response.getBody().get("id"));
    var id = UUID.fromString((String) response.getBody().get("id"));
    assertTrue(courseRepository.findById(id).isPresent());
    assertEquals(4, courseRepository.findById(id).get().getCredits());
  }

  @Test
  void createCourse_asTeacher_returnsForbidden() {
    var response = createCourse(token("TEACHER"), courseBody("COURSE-2", "Algebra", 4, "EL"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void createCourse_withoutToken_returnsUnauthorized() {
    var response = createCourse(null, courseBody("COURSE-3", "Algebra", 4, "EL"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void createCourse_missingFields_returnsBadRequest() {
    var body = Map.of("reference", "COURSE-4", "title", "Algebra", "credits", 4);

    var response = createCourse(token("ADMIN"), body);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createCourse_invalidCredits_returnsBadRequest() {
    var response = createCourse(token("ADMIN"), courseBody("COURSE-5", "Algebra", 0, "EL"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void updateCourse_asAdmin_returnsUpdated() {
    var course = saveCourse();
    var body = courseBody("COURSE-6", "Discrete Math", 6, "TN");

    var response = updateCourse(token("ADMIN"), course.getId(), body);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Discrete Math", response.getBody().get("title"));
    assertEquals("TN", response.getBody().get("parcours"));
    assertEquals(course.getId().toString(), response.getBody().get("id"));
    var saved = courseRepository.findById(course.getId()).get();
    assertEquals("Discrete Math", saved.getTitle());
    assertEquals(Parcours.TN, saved.getParcours());
  }

  @Test
  void updateCourse_onUnknownCourse_returnsNotFound() {
    var response =
        updateCourse(token("ADMIN"), UUID.randomUUID(), courseBody("COURSE-7", "Algebra", 4, "EL"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateCourse_asStudent_returnsForbidden() {
    var course = saveCourse();

    var response =
        updateCourse(token("STUDENT"), course.getId(), courseBody("COURSE-8", "Algebra", 4, "EL"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void listCourses_returnsCreatedCourseAndFiltersByParcours() {
    var elCourse = saveCourse("LIST-EL", Parcours.EL);
    saveCourse("LIST-TN", Parcours.TN);

    var all = getCourses(token("ADMIN"), null);
    assertEquals(HttpStatus.OK, all.getStatusCode());
    assertTrue(
        all.getBody().stream()
            .anyMatch(course -> course.get("id").equals(elCourse.getId().toString())));

    var tnOnly = getCourses(token("ADMIN"), "TN");
    assertEquals(HttpStatus.OK, tnOnly.getStatusCode());
    assertTrue(tnOnly.getBody().stream().allMatch(course -> "TN".equals(course.get("parcours"))));
    assertTrue(
        tnOnly.getBody().stream()
            .noneMatch(course -> course.get("id").equals(elCourse.getId().toString())));
  }

  @Test
  void listCourses_withoutToken_returnsUnauthorized() {
    var headers = new HttpHeaders();
    var response =
        restTemplate.exchange("/courses", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void listExams_returnsCourseExams() {
    var course = saveCourse();
    examRepository.save(
        JExam.builder()
            .course(course)
            .name("Midterm")
            .schedule(Instant.now())
            .coefficient(0.5)
            .build());

    var response = getExams(token("ADMIN"), course.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("Midterm", response.getBody().get(0).get("name"));
  }

  @Test
  void listExams_onUnknownCourse_returnsNotFound() {
    var response = getExamsAsString(token("ADMIN"), UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assignTeachers_asAdmin_returnsTeacherIds() {
    var course = saveCourse();
    var teacher = saveTeacher("courses-teacher");

    var response = assignTeachers(token("ADMIN"), course.getId(), List.of(teacher.getId()));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(teacher.getId().toString()), response.getBody().get("teacherIds"));
  }

  @Test
  void assignTeachers_onUnknownTeacher_returnsNotFound() {
    var course = saveCourse();

    var response = assignTeachers(token("ADMIN"), course.getId(), List.of(UUID.randomUUID()));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assignTeachers_asStudent_returnsForbidden() {
    var course = saveCourse();
    var teacher = saveTeacher("courses-teacher-forbidden");

    var response = assignTeachers(token("STUDENT"), course.getId(), List.of(teacher.getId()));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  private Map<String, Object> courseBody(
      String reference, String title, int credits, String parcours) {
    return Map.of("reference", reference, "title", title, "credits", credits, "parcours", parcours);
  }

  private JCourse saveCourse() {
    return saveCourse("C1", Parcours.EL);
  }

  private JCourse saveCourse(String reference, Parcours parcours) {
    return courseRepository.save(
        JCourse.builder()
            .reference(reference)
            .title("Mathematics")
            .credits(5)
            .parcours(parcours)
            .build());
  }

  private JTeacher saveTeacher(String username) {
    return teacherRepository.save(
        JTeacher.builder()
            .username(username)
            .password("password")
            .reference(username)
            .firstName("First")
            .lastName("Last")
            .build());
  }

  private ResponseEntity<Map> postExam(String token, UUID courseId, Object body) {
    var headers = headers(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/exams",
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        Map.class);
  }

  private ResponseEntity<Map> createCourse(String token, Object body) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/courses", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
  }

  private ResponseEntity<Map> updateCourse(String token, UUID courseId, Object body) {
    var headers = headers(token);
    return restTemplate.exchange(
        "/courses/" + courseId, HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
  }

  private ResponseEntity<List<Map>> getCourses(String token, String parcours) {
    var headers = headers(token);
    var uri = "/courses" + (parcours == null ? "" : "?parcours=" + parcours);
    return restTemplate.exchange(
        uri, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<List<Map>> getExams(String token, UUID courseId) {
    var headers = headers(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/exams",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<String> getExamsAsString(String token, UUID courseId) {
    var headers = headers(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/exams", HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private ResponseEntity<Map> assignTeachers(String token, UUID courseId, Object body) {
    var headers = headers(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/teachers",
        HttpMethod.PUT,
        new HttpEntity<>(Map.of("teacherIds", body), headers),
        Map.class);
  }

  private HttpHeaders headers(String token) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private String token(String role) {
    return jwtTokenProvider.generateToken("user", List.of(role));
  }
}
