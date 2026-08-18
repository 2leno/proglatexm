package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import api.poja.app.conf.FacadeIT;
import api.poja.app.conf.FakeBucketComponent;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.endpoint.event.model.PojaEvent;
import api.poja.app.endpoint.event.model.TranscriptGenerationRequested;
import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.mail.Email;
import api.poja.app.mail.Mailer;
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
import api.poja.app.service.event.TranscriptGenerationRequestedService;
import api.poja.app.service.event.TranscriptSendRequestedService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class TranscriptsIT extends FacadeIT {

  @MockBean EventProducer eventProducer;
  @MockBean Mailer mailer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JStudentRepository studentRepository;
  @Autowired JCourseRepository courseRepository;
  @Autowired JExamRepository examRepository;
  @Autowired JGradeRepository gradeRepository;
  @Autowired JPromotionRepository promotionRepository;
  @Autowired JGroupRepository groupRepository;
  @Autowired JStudentGroupPeriodRepository periodRepository;
  @Autowired TranscriptGenerationRequestedService generationHandler;
  @Autowired TranscriptSendRequestedService sendHandler;

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
  void generate_returns202Pending_thenHandlerProducesPdf() {
    var student = saveStudent("tr-gen-student");
    saveGrade(student, saveExam(saveCourse("TR-GEN-1", 5), 1.0, "2025-06-01T10:00:00Z"), 12.0);

    var generate = generate(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.ACCEPTED, generate.getStatusCode());
    assertEquals("PENDING", generate.getBody().get("status"));
    var event = producedEvents(TranscriptGenerationRequested.class).get(0);
    generationHandler.accept(event);

    var status = status(token("ADMIN"), student.getId(), 2025);
    assertEquals(HttpStatus.OK, status.getStatusCode());
    assertEquals("GENERATED", status.getBody().get("status"));
    assertNotNull(status.getBody().get("s3Key"));

    var download = download(token("ADMIN"), student.getId(), 2025);
    assertEquals(HttpStatus.OK, download.getStatusCode());
    assertTrue(new String(download.getBody(), StandardCharsets.UTF_8).startsWith("%PDF"));
  }

  @Test
  void generate_missingYear_returnsBadRequest() {
    var student = saveStudent("tr-no-year-student");

    var response = generate(token("ADMIN"), student.getId(), null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void generate_unknownStudent_returnsNotFound() {
    var response = generate(token("ADMIN"), UUID.randomUUID(), 2025);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void generate_asStudentOwner_returnsAccepted() {
    var student = saveStudent("tr-owner-student", "tr-owner", "tr-owner@proglatexm.com");

    var response = generate(token("tr-owner", "STUDENT"), student.getId(), 2025);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
  }

  @Test
  void generate_asStudentNonOwner_returnsForbidden() {
    var student = saveStudent("tr-non-owner-student");

    var response = generate(token("other-student", "STUDENT"), student.getId(), 2025);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void send_returns202_andHandlerSendsEmail() {
    var student = saveStudent("tr-send-student", "tr-send", "tr-send@proglatexm.com");
    saveGrade(student, saveExam(saveCourse("TR-SEND-1", 5), 1.0, "2025-06-01T10:00:00Z"), 12.0);
    var generate = generate(token("ADMIN"), student.getId(), 2025);
    generationHandler.accept(producedEvents(TranscriptGenerationRequested.class).get(0));
    assertEquals(
        "GENERATED", status(token("ADMIN"), student.getId(), 2025).getBody().get("status"));

    var send = send(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.ACCEPTED, send.getStatusCode());
    var sendEvent = producedEvents(TranscriptSendRequested.class).get(0);
    sendHandler.accept(sendEvent);

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    assertEquals("tr-send@proglatexm.com", emailCaptor.getValue().to().toString());
    assertEquals(1, emailCaptor.getValue().attachments().size());
    assertEquals("SENT", status(token("ADMIN"), student.getId(), 2025).getBody().get("status"));
  }

  @Test
  void send_unknownTranscript_returnsNotFound() {
    var student = saveStudent("tr-send-unknown-student");

    var response = send(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void sendAll_queuesGeneratedTranscriptsOnly() {
    var promotion = savePromotion("Tr Promotion 2025");
    var groupA = saveGroup("TR-A", promotion);
    var groupB = saveGroup("TR-B", promotion);
    var studentA = saveStudent("tr-all-a");
    var studentB = saveStudent("tr-all-b");
    var studentC = saveStudent("tr-all-c");
    periodRepository.save(period(studentA, groupA));
    periodRepository.save(period(studentB, groupB));
    periodRepository.save(period(studentC, groupA));
    var exam = saveExam(saveCourse("TR-ALL-1", 5), 1.0, "2025-06-01T10:00:00Z");
    saveGrade(studentA, exam, 12.0);
    saveGrade(studentB, exam, 14.0);
    saveGrade(studentC, exam, 16.0);
    generate(token("ADMIN"), studentA.getId(), 2025);
    generationHandler.accept(lastGenerationEvent());
    generate(token("ADMIN"), studentB.getId(), 2025);
    generationHandler.accept(lastGenerationEvent());

    var response = sendAll(token("ADMIN"), promotion.getId(), 2025);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody().get("batchId"));
    assertEquals(2, response.getBody().get("studentsQueued"));
    var sendEvents = producedEvents(TranscriptSendRequested.class);
    assertEquals(2, sendEvents.size());
    sendEvents.forEach(sendHandler::accept);
    assertEquals("SENT", status(token("ADMIN"), studentA.getId(), 2025).getBody().get("status"));
    assertEquals("SENT", status(token("ADMIN"), studentB.getId(), 2025).getBody().get("status"));
  }

  @Test
  void sendAll_unknownPromotion_returnsNotFound() {
    var response = sendAll(token("ADMIN"), UUID.randomUUID(), 2025);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void status_returnsPendingAfterGenerate() {
    var student = saveStudent("tr-status-student");
    saveGrade(student, saveExam(saveCourse("TR-STATUS-1", 5), 1.0, "2025-06-01T10:00:00Z"), 12.0);
    generate(token("ADMIN"), student.getId(), 2025);

    var response = status(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("PENDING", response.getBody().get("status"));
    assertEquals(2025, response.getBody().get("year"));
  }

  @Test
  void status_unknownTranscript_returnsNotFound() {
    var student = saveStudent("tr-status-unknown-student");

    var response = status(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void download_unknownTranscript_returnsNotFound() {
    var student = saveStudent("tr-download-unknown-student");

    var response = download(token("ADMIN"), student.getId(), 2025);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  private <T extends PojaEvent> List<T> producedEvents(Class<T> type) {
    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, atLeastOnce()).accept(captor.capture());
    return captor.getAllValues().stream()
        .flatMap(Collection::stream)
        .filter(type::isInstance)
        .map(e -> (T) e)
        .toList();
  }

  private TranscriptGenerationRequested lastGenerationEvent() {
    var events = producedEvents(TranscriptGenerationRequested.class);
    return events.get(events.size() - 1);
  }

  private JStudent saveStudent(String reference) {
    return saveStudent(reference, reference, reference + "@proglatexm.com");
  }

  private JStudent saveStudent(String reference, String username, String email) {
    return studentRepository.save(
        JStudent.builder()
            .username(username)
            .password("password")
            .firstName("First")
            .lastName("Last")
            .reference(reference)
            .parcours(Parcours.EL)
            .email(email)
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
    return gradeRepository.save(
        JGrade.builder().student(student).exam(exam).value(value).current(true).build());
  }

  private JPromotion savePromotion(String name) {
    return promotionRepository.save(JPromotion.builder().name(name).year(2025).build());
  }

  private JGroup saveGroup(String reference, JPromotion promotion) {
    return groupRepository.save(JGroup.builder().reference(reference).promotion(promotion).build());
  }

  private JStudentGroupPeriod period(JStudent student, JGroup group) {
    return JStudentGroupPeriod.builder()
        .student(student)
        .group(group)
        .startDate(LocalDate.of(2025, 9, 1))
        .build();
  }

  private ResponseEntity<Map> generate(String token, UUID studentId, Integer year) {
    var uri =
        "/students/" + studentId + "/transcripts/generate" + (year == null ? "" : "?year=" + year);
    return restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(headers(token)), Map.class);
  }

  private ResponseEntity<Map> send(String token, UUID studentId, Integer year) {
    return restTemplate.exchange(
        "/students/" + studentId + "/transcripts/send?year=" + year,
        HttpMethod.POST,
        new HttpEntity<>(headers(token)),
        Map.class);
  }

  private ResponseEntity<Map> sendAll(String token, UUID promotionId, Integer year) {
    return restTemplate.exchange(
        "/promotions/" + promotionId + "/transcripts/send-all?year=" + year,
        HttpMethod.POST,
        new HttpEntity<>(headers(token)),
        Map.class);
  }

  private ResponseEntity<Map> status(String token, UUID studentId, Integer year) {
    return restTemplate.exchange(
        "/students/" + studentId + "/transcripts/" + year + "/status",
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        Map.class);
  }

  private ResponseEntity<byte[]> download(String token, UUID studentId, Integer year) {
    return restTemplate.exchange(
        "/students/" + studentId + "/transcripts/" + year,
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        byte[].class);
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
