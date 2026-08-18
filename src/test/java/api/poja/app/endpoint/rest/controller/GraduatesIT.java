package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.app.conf.FacadeIT;
import api.poja.app.conf.FakeBucketComponent;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JGroupRepository;
import api.poja.app.repository.JPromotionRepository;
import api.poja.app.repository.JStudentGroupPeriodRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JExam;
import api.poja.app.repository.model.JGrade;
import api.poja.app.repository.model.JGroup;
import api.poja.app.repository.model.JPromotion;
import api.poja.app.repository.model.JStudent;
import api.poja.app.repository.model.JStudentGroupPeriod;
import api.poja.app.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class GraduatesIT extends FacadeIT {

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JStudentRepository studentRepository;
  @Autowired JCourseRepository courseRepository;
  @Autowired JExamRepository examRepository;
  @Autowired JGradeRepository gradeRepository;
  @Autowired JPromotionRepository promotionRepository;
  @Autowired JGroupRepository groupRepository;
  @Autowired JStudentGroupPeriodRepository periodRepository;

  @TestConfiguration
  static class TestBeans {

    @Bean
    @Primary
    BucketComponent bucketComponent() {
      return new FakeBucketComponent();
    }
  }

  @BeforeEach
  void disableStreaming() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @Test
  void generate_returnsExcel_withOnlyAllCoursePassers() {
    var promotion = savePromotion("Graduates Promotion 2026");
    var group = saveGroup("GR-A", promotion);
    var passingStudent = saveStudent("gr-passing");
    var failingStudent = saveStudent("gr-failing");
    periodRepository.save(period(passingStudent, group));
    periodRepository.save(period(failingStudent, group));
    var course1 = saveCourse("GR-C1", 5);
    var course2 = saveCourse("GR-C2", 5);
    saveGrade(passingStudent, saveExam(course1, 1.0, "2024-06-01T10:00:00Z"), 15.0);
    saveGrade(passingStudent, saveExam(course2, 1.0, "2025-06-01T10:00:00Z"), 13.0);
    saveGrade(failingStudent, saveExam(course1, 1.0, "2024-06-01T10:00:00Z"), 8.0);
    saveGrade(failingStudent, saveExam(course2, 1.0, "2025-06-01T10:00:00Z"), 15.0);

    var response = generate(token("ADMIN"), promotion.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody().get("fileKey"));
    assertEquals(1, response.getBody().get("graduatesCount"));
  }

  @Test
  void generate_unknownPromotion_returnsNotFound() {
    var response = generate(token("ADMIN"), UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void download_returnsXlsx_afterGeneration() {
    var promotion = savePromotion("Graduates Download 2026");
    var group = saveGroup("GR-DL", promotion);
    var student = saveStudent("gr-download");
    periodRepository.save(period(student, group));
    var course1 = saveCourse("GR-DLC1", 5);
    var course2 = saveCourse("GR-DLC2", 5);
    saveGrade(student, saveExam(course1, 1.0, "2024-06-01T10:00:00Z"), 15.0);
    saveGrade(student, saveExam(course2, 1.0, "2025-06-01T10:00:00Z"), 13.0);
    generate(token("ADMIN"), promotion.getId());

    var download = download(token("ADMIN"), promotion.getId());

    assertEquals(HttpStatus.OK, download.getStatusCode());
    var content = new String(download.getBody(), StandardCharsets.ISO_8859_1);
    assertTrue(content.startsWith("PK"));
  }

  @Test
  void download_withoutGeneration_returnsNotFound() {
    var promotion = savePromotion("Graduates NoFile 2026");

    var download = download(token("ADMIN"), promotion.getId());

    assertEquals(HttpStatus.NOT_FOUND, download.getStatusCode());
  }

  @Test
  void list_returnsRankedGraduates() {
    var promotion = savePromotion("Graduates List 2026");
    var group = saveGroup("GR-L", promotion);
    var bestStudent = saveStudent("gr-best");
    var secondStudent = saveStudent("gr-second");
    periodRepository.save(period(bestStudent, group));
    periodRepository.save(period(secondStudent, group));
    var course1 = saveCourse("GR-LC1", 5);
    var course2 = saveCourse("GR-LC2", 5);
    saveGrade(bestStudent, saveExam(course1, 1.0, "2024-06-01T10:00:00Z"), 15.0);
    saveGrade(bestStudent, saveExam(course2, 1.0, "2025-06-01T10:00:00Z"), 14.0);
    saveGrade(secondStudent, saveExam(course1, 1.0, "2024-06-01T10:00:00Z"), 11.0);
    saveGrade(secondStudent, saveExam(course2, 1.0, "2025-06-01T10:00:00Z"), 12.0);

    var response = list(token("ADMIN"), promotion.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var graduates = response.getBody();
    assertEquals(2, graduates.size());
    assertEquals(1, ((Map) graduates.get(0)).get("rank"));
    assertEquals("gr-best", ((Map) graduates.get(0)).get("reference"));
    assertEquals(2, ((Map) graduates.get(1)).get("rank"));
    assertEquals("gr-second", ((Map) graduates.get(1)).get("reference"));
    assertTrue(
        ((Number) ((Map) graduates.get(0)).get("generalAverage")).doubleValue()
            > ((Number) ((Map) graduates.get(1)).get("generalAverage")).doubleValue());
  }

  @Test
  void list_excludesLowUE_evenWithHighGeneralAverage() {
    var promotion = savePromotion("Graduates Criterion 2026");
    var group = saveGroup("GR-CR", promotion);
    var student = saveStudent("gr-criterion");
    periodRepository.save(period(student, group));
    var course1 = saveCourse("GR-CRC1", 5);
    var course2 = saveCourse("GR-CRC2", 5);
    saveGrade(student, saveExam(course1, 1.0, "2024-06-01T10:00:00Z"), 8.0);
    saveGrade(student, saveExam(course2, 1.0, "2025-06-01T10:00:00Z"), 15.0);

    var response = list(token("ADMIN"), promotion.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(0, response.getBody().size());
  }

  @Test
  void list_unknownPromotion_returnsNotFound() {
    var response = listAsMap(token("ADMIN"), UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void generate_asTeacher_returnsForbidden() {
    var promotion = savePromotion("Graduates Sec 2026");

    var response = generate(token("teacher", "TEACHER"), promotion.getId());

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  private JStudent saveStudent(String reference) {
    return studentRepository.save(
        JStudent.builder()
            .username(reference)
            .password("password")
            .firstName("First")
            .lastName("Last")
            .reference(reference)
            .parcours(Parcours.EL)
            .email(reference + "@proglatexm.com")
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
            .name("Final")
            .schedule(Instant.parse(schedule))
            .coefficient(coefficient)
            .build());
  }

  private JGrade saveGrade(JStudent student, JExam exam, double value) {
    return gradeRepository.save(
        JGrade.builder().student(student).exam(exam).value(value).current(true).build());
  }

  private JPromotion savePromotion(String name) {
    return promotionRepository.save(JPromotion.builder().name(name).year(2026).build());
  }

  private JGroup saveGroup(String reference, JPromotion promotion) {
    return groupRepository.save(JGroup.builder().reference(reference).promotion(promotion).build());
  }

  private JStudentGroupPeriod period(JStudent student, JGroup group) {
    return JStudentGroupPeriod.builder()
        .student(student)
        .group(group)
        .startDate(LocalDate.of(2026, 9, 1))
        .build();
  }

  private ResponseEntity<Map> generate(String token, UUID promotionId) {
    return restTemplate.exchange(
        "/promotions/" + promotionId + "/graduates/generate",
        HttpMethod.POST,
        new HttpEntity<>(headers(token)),
        Map.class);
  }

  private ResponseEntity<byte[]> download(String token, UUID promotionId) {
    return restTemplate.exchange(
        "/promotions/" + promotionId + "/graduates/download",
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        byte[].class);
  }

  @SuppressWarnings("unchecked")
  private ResponseEntity<List> list(String token, UUID promotionId) {
    return restTemplate.exchange(
        "/promotions/" + promotionId + "/graduates",
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        List.class);
  }

  private ResponseEntity<Map> listAsMap(String token, UUID promotionId) {
    return restTemplate.exchange(
        "/promotions/" + promotionId + "/graduates",
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        Map.class);
  }

  private HttpHeaders headers(String token) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private String token(String role) {
    return token("user", role);
  }

  private String token(String username, String role) {
    return jwtTokenProvider.generateToken(username, List.of(role));
  }
}
