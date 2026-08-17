package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.JGradeHistoryRepository;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.JTeacherRepository;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JExam;
import api.poja.app.repository.model.JStudent;
import api.poja.app.repository.model.JTeacher;
import api.poja.app.security.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

class GradesIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JStudentRepository studentRepository;
  @Autowired JCourseRepository courseRepository;
  @Autowired JExamRepository examRepository;
  @Autowired JGradeRepository gradeRepository;
  @Autowired JGradeHistoryRepository gradeHistoryRepository;
  @Autowired JTeacherRepository teacherRepository;

  @Test
  void recordGrade_asAdmin_returnsCreatedAndPersists() {
    var student = saveStudent("gr-admin-student");
    var exam = saveExam(saveCourse("grade-admin-course"));

    var response = recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 15.0));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(15.0, (Double) response.getBody().get("value"));
    assertTrue(response.getBody().get("current").equals(true));
    assertNotNull(response.getBody().get("id"));
    var saved = gradeRepository.findByStudentIdAndCurrentTrue(student.getId());
    assertEquals(1, saved.size());
    var history = gradeHistoryRepository.findByGradeIdOrderByModifiedAtDesc(saved.get(0).getId());
    assertEquals(1, history.size());
    assertEquals("Initial grade", history.get(0).getReason());
  }

  @Test
  void recordGrade_asCourseTeacher_returnsCreated() {
    var teacher = saveTeacher("gr-course-teacher");
    var course = saveCourse("grade-owner-course");
    course.getTeachers().add(teacher);
    courseRepository.save(course);
    var student = saveStudent("gr-owner-student");
    var exam = saveExam(course);

    var response =
        recordGrade(token("gr-course-teacher", "TEACHER"), exam, gradeBody(student.getId(), 12.0));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  @Test
  void recordGrade_asNonOwnerTeacher_returnsForbidden() {
    saveTeacher("gr-non-owner-teacher");
    var otherTeacher = saveTeacher("gr-course-owner-teacher");
    var course = saveCourse("grade-non-owner-course");
    course.getTeachers().add(otherTeacher);
    courseRepository.save(course);
    var student = saveStudent("gr-nonowner-student");
    var exam = saveExam(course);

    var response =
        recordGrade(
            token("gr-non-owner-teacher", "TEACHER"), exam, gradeBody(student.getId(), 12.0));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void recordGrade_onUnknownExam_returnsNotFound() {
    var student = saveStudent("gr-unknown-exam-student");
    var course = saveCourse("grade-unknown-exam-course");
    var unknownExam = examBuilder(course).id(UUID.randomUUID()).build();

    var response = recordGrade(token("ADMIN"), unknownExam, gradeBody(student.getId(), 12.0));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void recordGrade_whenExamNotInCourse_returnsNotFound() {
    var student = saveStudent("gr-mismatch-student");
    var exam = saveExam(saveCourse("grade-mismatch-course"));

    var response =
        recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 12.0), UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void recordGrade_onUnknownStudent_returnsNotFound() {
    var exam = saveExam(saveCourse("grade-unknown-student-course"));

    var response = recordGrade(token("ADMIN"), exam, gradeBody(UUID.randomUUID(), 12.0));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void recordGrade_valueOutOfRange_returnsBadRequest() {
    var student = saveStudent("gr-range-student");
    var exam = saveExam(saveCourse("grade-range-course"));

    var response = recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 21.0));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void recordGrade_reRecordMarksPreviousGradeNonCurrent() {
    var student = saveStudent("gr-rerecord-student");
    var exam = saveExam(saveCourse("grade-rerecord-course"));

    recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 10.0));
    var response = recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 14.0));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var current = gradeRepository.findByStudentIdAndCurrentTrue(student.getId());
    assertEquals(1, current.size());
    assertEquals(14.0, current.get(0).getValue());
    var all = gradeRepository.findByStudentIdAndExamId(student.getId(), exam.getId());
    assertEquals(2, all.size());
    assertEquals(1, all.stream().filter(grade -> Boolean.TRUE.equals(grade.getCurrent())).count());
  }

  @Test
  void getGrades_asAdmin_returnsCurrentGradesAndFiltersByCourse() {
    var student = saveStudent("gr-grades-admin-student");
    var course = saveCourse("grades-admin-course");
    var otherCourse = saveCourse("grades-admin-other");
    var exam = saveExam(course);
    var otherExam = saveExam(otherCourse);
    recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 15.0));
    recordGrade(token("ADMIN"), otherExam, gradeBody(student.getId(), 11.0));

    var filtered = getGrades(token("ADMIN"), student.getId(), course.getId());

    assertEquals(HttpStatus.OK, filtered.getStatusCode());
    assertEquals(1, filtered.getBody().size());
    assertEquals(15.0, (Double) filtered.getBody().get(0).get("value"));

    var all = getGrades(token("ADMIN"), student.getId(), null);
    assertEquals(2, all.getBody().size());
  }

  @Test
  void getGrades_asStudentOwner_returnsOk() {
    var student = saveStudent("gr-grades-owner-student", "gr-grades-owner");
    var exam = saveExam(saveCourse("gr-grades-owner-course"));
    recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 13.0));

    var response = getGrades(token("gr-grades-owner", "STUDENT"), student.getId(), null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getGrades_asStudentNonOwner_returnsForbidden() {
    saveStudent("gr-grades-nonowner-student", "gr-grades-nonowner");

    var response = getGradesAsString(token("gr-other-user", "STUDENT"), UUID.randomUUID(), null);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void getGradeHistory_asAdmin_returnsEntriesOrdered() {
    var student = saveStudent("gr-history-admin-student");
    var exam = saveExam(saveCourse("history-admin-course"));
    recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 10.0));
    var grade = gradeRepository.findByStudentIdAndCurrentTrue(student.getId()).get(0);
    modifyGrade(
        token("ADMIN"), grade.getId(), Map.of("newValue", 14.0, "reason", "Claim accepted"));

    var response = getGradeHistory(token("ADMIN"), student.getId(), grade.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
    assertEquals("Claim accepted", response.getBody().get(0).get("reason"));
    assertEquals(14.0, (Double) response.getBody().get(0).get("value"));
  }

  @Test
  void getGradeHistory_whenGradeNotForStudent_returnsNotFound() {
    var student = saveStudent("gr-history-mismatch-student");
    var otherStudent = saveStudent("gr-history-mismatch-other");
    var exam = saveExam(saveCourse("history-mismatch-course"));
    recordGrade(token("ADMIN"), exam, gradeBody(otherStudent.getId(), 10.0));
    var grade = gradeRepository.findByStudentIdAndCurrentTrue(otherStudent.getId()).get(0);

    var response = getGradeHistoryAsString(token("ADMIN"), student.getId(), grade.getId());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void modifyGrade_createsNewHistoryVersion() {
    var student = saveStudent("gr-modify-student");
    var exam = saveExam(saveCourse("modify-course"));
    recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 10.0));
    var grade = gradeRepository.findByStudentIdAndCurrentTrue(student.getId()).get(0);

    var response =
        modifyGrade(token("ADMIN"), grade.getId(), Map.of("newValue", 16.0, "reason", "Remarking"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(16.0, (Double) response.getBody().get("value"));
    var history = gradeHistoryRepository.findByGradeIdOrderByModifiedAtDesc(grade.getId());
    assertEquals(2, history.size());
    assertEquals("Remarking", history.get(0).getReason());
    assertEquals(16.0, history.get(0).getValue());
    assertFalse(history.get(0).getModifiedBy().isBlank());
  }

  @Test
  void modifyGrade_asNonOwnerTeacher_returnsForbidden() {
    saveTeacher("gr-modify-non-owner-teacher");
    var otherTeacher = saveTeacher("gr-modify-course-owner-teacher");
    var course = saveCourse("modify-non-owner-course");
    course.getTeachers().add(otherTeacher);
    courseRepository.save(course);
    var student = saveStudent("gr-modify-nonowner-student");
    var exam = saveExam(course);
    recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 10.0));
    var grade = gradeRepository.findByStudentIdAndCurrentTrue(student.getId()).get(0);

    var response =
        modifyGrade(
            token("gr-modify-non-owner-teacher", "TEACHER"),
            grade.getId(),
            Map.of("newValue", 16.0, "reason", "Remarking"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void modifyGrade_missingReason_returnsBadRequest() {
    var student = saveStudent("gr-modify-reason-student");
    var exam = saveExam(saveCourse("modify-reason-course"));
    recordGrade(token("ADMIN"), exam, gradeBody(student.getId(), 10.0));
    var grade = gradeRepository.findByStudentIdAndCurrentTrue(student.getId()).get(0);

    var response =
        modifyGrade(token("ADMIN"), grade.getId(), Map.of("newValue", 16.0, "reason", ""));

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

  private JCourse saveCourse(String reference) {
    return courseRepository.save(
        JCourse.builder()
            .reference(reference)
            .title("Mathematics")
            .credits(5)
            .parcours(Parcours.EL)
            .build());
  }

  private JExam saveExam(JCourse course) {
    return examRepository.save(examBuilder(course).build());
  }

  private JExam.JExamBuilder examBuilder(JCourse course) {
    return JExam.builder().course(course).name("Midterm").schedule(Instant.now()).coefficient(0.5);
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

  private Map<String, Object> gradeBody(UUID studentId, Double value) {
    return Map.of("studentId", studentId.toString(), "value", value);
  }

  private ResponseEntity<Map> recordGrade(String token, JExam exam, Object body) {
    return recordGrade(token, exam, body, exam.getCourse().getId());
  }

  private ResponseEntity<Map> recordGrade(String token, JExam exam, Object body, UUID courseId) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/courses/" + courseId + "/exams/" + exam.getId() + "/grades",
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        Map.class);
  }

  private ResponseEntity<List<Map>> getGrades(String token, UUID studentId, UUID courseId) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    var uri =
        "/students/" + studentId + "/grades" + (courseId == null ? "" : "?courseId=" + courseId);
    return restTemplate.exchange(
        uri,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<List<Map>>() {});
  }

  private ResponseEntity<String> getGradesAsString(String token, UUID studentId, UUID courseId) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    var uri =
        "/students/" + studentId + "/grades" + (courseId == null ? "" : "?courseId=" + courseId);
    return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private ResponseEntity<List<Map>> getGradeHistory(String token, UUID studentId, UUID gradeId) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/grades/" + gradeId + "/history",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<List<Map>>() {});
  }

  private ResponseEntity<String> getGradeHistoryAsString(
      String token, UUID studentId, UUID gradeId) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/grades/" + gradeId + "/history",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);
  }

  private ResponseEntity<Map> modifyGrade(String token, UUID gradeId, Object body) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/grades/" + gradeId, HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
  }

  private String token(String role) {
    return token("user", role);
  }

  private String token(String username, String role) {
    return jwtTokenProvider.generateToken(username, List.of(role));
  }
}
