package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JExam;
import api.poja.app.repository.model.JGrade;
import api.poja.app.repository.model.JStudent;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class AveragesIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JStudentRepository studentRepository;
  @Autowired JCourseRepository courseRepository;
  @Autowired JExamRepository examRepository;
  @Autowired JGradeRepository gradeRepository;

  @BeforeEach
  void disableStreaming() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @Test
  void annualAverage_returnsWeightedAverageAndCredits() {
    var student = saveStudent("avg-weighted-student");
    var course = saveCourse("AVG-W1", 5);
    saveGrade(student, saveExam(course, 0.5, "2025-06-01T10:00:00Z"), 12.0);
    saveGrade(student, saveExam(course, 0.5, "2025-06-15T10:00:00Z"), 16.0);

    var response = annualAverage(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2025, response.getBody().get("year"));
    assertEquals(14.0, response.getBody().get("average"));
    assertEquals(5, response.getBody().get("credits"));
  }

  @Test
  void annualAverage_filtersByYearAndSumsDistinctCourseCredits() {
    var student = saveStudent("avg-year-student");
    var course2025 = saveCourse("AVG-Y1", 5);
    var course2026 = saveCourse("AVG-Y2", 3);
    saveGrade(student, saveExam(course2025, 1.0, "2025-06-01T10:00:00Z"), 10.0);
    saveGrade(student, saveExam(course2026, 1.0, "2026-06-01T10:00:00Z"), 20.0);

    var year2025 = annualAverage(token("ADMIN"), student.getId(), 2025);
    var year2026 = annualAverage(token("ADMIN"), student.getId(), 2026);

    assertEquals(HttpStatus.OK, year2025.getStatusCode());
    assertEquals(10.0, year2025.getBody().get("average"));
    assertEquals(5, year2025.getBody().get("credits"));
    assertEquals(20.0, year2026.getBody().get("average"));
    assertEquals(3, year2026.getBody().get("credits"));
  }

  @Test
  void annualAverage_emptyYear_returnsZero() {
    var student = saveStudent("avg-empty-student");
    saveGrade(student, saveExam(saveCourse("AVG-E1", 5), 1.0, "2025-06-01T10:00:00Z"), 12.0);

    var response = annualAverage(token("ADMIN"), student.getId(), 2030);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2030, response.getBody().get("year"));
    assertEquals(0.0, response.getBody().get("average"));
    assertEquals(0, response.getBody().get("credits"));
  }

  @Test
  void annualAverage_ignoresNonCurrentGrades() {
    var student = saveStudent("avg-current-student");
    var exam = saveExam(saveCourse("AVG-C1", 5), 1.0, "2025-06-01T10:00:00Z");
    saveGrade(student, exam, 4.0, false);
    saveGrade(student, exam, 16.0, true);

    var response = annualAverage(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(16.0, response.getBody().get("average"));
  }

  @Test
  void globalAverage_usesAllYears() {
    var student = saveStudent("avg-global-student");
    saveGrade(student, saveExam(saveCourse("AVG-G1", 5), 1.0, "2025-06-01T10:00:00Z"), 10.0);
    saveGrade(student, saveExam(saveCourse("AVG-G2", 3), 1.0, "2026-06-01T10:00:00Z"), 20.0);

    var response = globalAverage(token("ADMIN"), student.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(15.0, response.getBody().get("average"));
  }

  @Test
  void annualAverage_asStudentOwner_returnsOk() {
    var student = saveStudent("avg-owner-student", "avg-owner");
    saveGrade(student, saveExam(saveCourse("AVG-O1", 5), 1.0, "2025-06-01T10:00:00Z"), 12.0);

    var response = annualAverage(token("avg-owner", "STUDENT"), student.getId(), 2025);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void annualAverage_asStudentNonOwner_returnsForbidden() {
    var student = saveStudent("avg-non-owner-student", "avg-non-owner");

    var response = annualAverageAsString(token("other-user", "STUDENT"), student.getId(), 2025);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void annualAverage_asTeacher_returnsOk() {
    var student = saveStudent("avg-teacher-student");
    saveGrade(student, saveExam(saveCourse("AVG-T1", 5), 1.0, "2025-06-01T10:00:00Z"), 12.0);

    var response = annualAverage(token("TEACHER"), student.getId(), 2025);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void annualAverage_withoutToken_returnsUnauthorized() {
    var response = annualAverageAsString(null, UUID.randomUUID(), 2025);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void annualAverage_onUnknownStudent_returnsNotFound() {
    var response = annualAverageAsString(token("ADMIN"), UUID.randomUUID(), 2025);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void annualAverage_missingYear_returnsBadRequest() {
    var student = saveStudent("avg-no-year-student");

    var response = annualAverageAsString(token("ADMIN"), student.getId(), null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  private JStudent saveStudent(String reference) {
    return saveStudent(reference, reference);
  }

  private JStudent saveStudent(String reference, String username) {
    return studentRepository.save(
        JStudent.builder()
            .username(username)
            .password("password")
            .firstName("First")
            .lastName("Last")
            .reference(reference)
            .parcours(Parcours.EL)
            .build());
  }

  private JCourse saveCourse(String reference, int credits) {
    return courseRepository.save(
        JCourse.builder()
            .reference(reference)
            .title("Mathematics")
            .credits(credits)
            .parcours(Parcours.EL)
            .build());
  }

  private JExam saveExam(JCourse course, double coefficient, String schedule) {
    return examRepository.save(
        JExam.builder()
            .course(course)
            .name("Midterm")
            .schedule(Instant.parse(schedule))
            .coefficient(coefficient)
            .build());
  }

  private JGrade saveGrade(JStudent student, JExam exam, double value) {
    return saveGrade(student, exam, value, true);
  }

  private JGrade saveGrade(JStudent student, JExam exam, double value, boolean current) {
    return gradeRepository.save(
        JGrade.builder().student(student).exam(exam).value(value).current(current).build());
  }

  private ResponseEntity<Map> annualAverage(String token, UUID studentId, Integer year) {
    var uri = "/students/" + studentId + "/average" + (year == null ? "" : "?year=" + year);
    return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers(token)), Map.class);
  }

  private ResponseEntity<String> annualAverageAsString(String token, UUID studentId, Integer year) {
    var uri = "/students/" + studentId + "/average" + (year == null ? "" : "?year=" + year);
    return restTemplate.exchange(
        uri, HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
  }

  private ResponseEntity<Map> globalAverage(String token, UUID studentId) {
    return restTemplate.exchange(
        "/students/" + studentId + "/average/global",
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        Map.class);
  }

  private HttpHeaders headers(String token) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  private String token(String role) {
    return token("user", role);
  }

  private String token(String username, String role) {
    return jwtTokenProvider.generateToken(username, List.of(role));
  }
}
