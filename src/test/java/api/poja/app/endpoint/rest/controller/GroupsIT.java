package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JGroupRepository;
import api.poja.app.repository.JPromotionRepository;
import api.poja.app.repository.JStudentGroupPeriodRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.model.JGroup;
import api.poja.app.repository.model.JPromotion;
import api.poja.app.repository.model.JStudent;
import api.poja.app.repository.model.JStudentGroupPeriod;
import api.poja.app.security.JwtTokenProvider;
import java.time.LocalDate;
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

class GroupsIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JStudentRepository studentRepository;
  @Autowired JGroupRepository groupRepository;
  @Autowired JPromotionRepository promotionRepository;
  @Autowired JStudentGroupPeriodRepository periodRepository;

  @Test
  void assign_asAdmin_returnsCreatedAndPersists() {
    var student = saveStudent("assign-admin-student");
    var group = saveGroup("assign-admin-group");
    var body = Map.of("groupId", group.getId().toString(), "effectiveDate", "2025-01-10");

    var response = assign(token("ADMIN"), student.getId(), body);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(group.getReference(), response.getBody().get("reference"));
    assertEquals("2025-01-10", response.getBody().get("startDate"));
    var saved = periodRepository.findByStudentIdOrderByStartDateAsc(student.getId());
    assertEquals(1, saved.size());
    assertEquals(group.getId(), saved.get(0).getGroup().getId());
    assertNull(saved.get(0).getEndDate());
  }

  @Test
  void assign_whenPeriodOpen_closesPrevious() {
    var student = saveStudent("assign-close-student");
    var firstGroup = saveGroup("assign-close-first");
    var secondGroup = saveGroup("assign-close-second");
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(firstGroup)
            .startDate(LocalDate.of(2025, 1, 10))
            .build());

    var response =
        assign(
            token("ADMIN"),
            student.getId(),
            Map.of("groupId", secondGroup.getId().toString(), "effectiveDate", "2025-06-01"));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(secondGroup.getReference(), response.getBody().get("reference"));
    var saved = periodRepository.findByStudentIdOrderByStartDateAsc(student.getId());
    assertEquals(2, saved.size());
    assertEquals(LocalDate.of(2025, 5, 31), saved.get(0).getEndDate());
    assertEquals(LocalDate.of(2025, 6, 1), saved.get(1).getStartDate());
    assertNull(saved.get(1).getEndDate());
  }

  @Test
  void assign_onUnknownStudent_returnsNotFound() {
    var group = saveGroup("assign-unknown-student-group");

    var response =
        assign(
            token("ADMIN"),
            UUID.randomUUID(),
            Map.of("groupId", group.getId().toString(), "effectiveDate", "2025-01-10"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assign_onUnknownGroup_returnsNotFound() {
    var student = saveStudent("assign-unknown-group-student");

    var response =
        assign(
            token("ADMIN"),
            student.getId(),
            Map.of("groupId", UUID.randomUUID().toString(), "effectiveDate", "2025-01-10"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assign_whenDateNotAfterLastPeriod_returnsConflict() {
    var student = saveStudent("assign-conflict-student");
    var firstGroup = saveGroup("assign-conflict-first");
    var secondGroup = saveGroup("assign-conflict-second");
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(firstGroup)
            .startDate(LocalDate.of(2025, 6, 1))
            .build());

    var response =
        assign(
            token("ADMIN"),
            student.getId(),
            Map.of("groupId", secondGroup.getId().toString(), "effectiveDate", "2025-06-01"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void assign_withInvalidGroupId_returnsBadRequest() {
    var student = saveStudent("assign-invalid-group-student");

    var response =
        assign(
            token("ADMIN"),
            student.getId(),
            Map.of("groupId", "not-a-uuid", "effectiveDate", "2025-01-10"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void history_asAdmin_returnsOrderedPeriods() {
    var student = saveStudent("history-admin-student");
    var firstGroup = saveGroup("history-admin-first");
    var secondGroup = saveGroup("history-admin-second");
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(firstGroup)
            .startDate(LocalDate.of(2025, 1, 10))
            .endDate(LocalDate.of(2025, 5, 31))
            .build());
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(secondGroup)
            .startDate(LocalDate.of(2025, 6, 1))
            .build());

    var response = history(token("ADMIN"), student.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
    assertEquals(firstGroup.getReference(), response.getBody().get(0).get("reference"));
    assertEquals(secondGroup.getReference(), response.getBody().get(1).get("reference"));
  }

  @Test
  void history_asStudentOwner_returnsOk() {
    var student = saveStudent("history-owner-student", "history-owner");
    var group = saveGroup("history-owner-group");
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(group)
            .startDate(LocalDate.of(2025, 1, 10))
            .build());

    var response = history(token("history-owner", "STUDENT"), student.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void history_asStudentNonOwner_returnsForbidden() {
    var student = saveStudent("history-non-owner-student", "history-non-owner");

    var response = historyAsString(token("other-user", "STUDENT"), student.getId());

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void history_onUnknownStudent_returnsNotFound() {
    var response = historyAsString(token("ADMIN"), UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void history_withoutPeriods_returnsEmptyList() {
    var student = saveStudent("history-empty-student");

    var response = history(token("ADMIN"), student.getId());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().size());
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

  private JGroup saveGroup(String reference) {
    var promotion =
        promotionRepository.save(JPromotion.builder().name("Promotion 2025").year(2025).build());
    return groupRepository.save(JGroup.builder().reference(reference).promotion(promotion).build());
  }

  private ResponseEntity<Map> assign(String token, UUID studentId, Object body) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/students/" + studentId + "/groups",
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        Map.class);
  }

  private ResponseEntity<List<Map>> history(String token, UUID studentId) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/groups/history",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<List<Map>>() {});
  }

  private ResponseEntity<String> historyAsString(String token, UUID studentId) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/groups/history",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);
  }

  private String token(String role) {
    return token("user", role);
  }

  private String token(String username, String role) {
    return jwtTokenProvider.generateToken(username, List.of(role));
  }
}
