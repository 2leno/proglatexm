package api.poja.app.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class GroupsIT extends FacadeIT {

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JwtTokenProvider jwtTokenProvider;
  @Autowired JStudentRepository studentRepository;
  @Autowired JGroupRepository groupRepository;
  @Autowired JPromotionRepository promotionRepository;
  @Autowired JStudentGroupPeriodRepository periodRepository;

  @BeforeEach
  void disableStreaming() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

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

  @Test
  void createGroup_asAdmin_returnsCreatedAndPersists() {
    var promotion = savePromotion("create-group-promotion");
    var body =
        Map.of("reference", "create-group-reference", "promotionId", promotion.getId().toString());

    var response = createGroup(token("ADMIN"), body);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("create-group-reference", response.getBody().get("reference"));
    assertEquals(promotion.getId().toString(), response.getBody().get("promotionId"));
    var references = groupRepository.findAll().stream().map(JGroup::getReference).toList();
    assertTrue(references.contains("create-group-reference"));
  }

  @Test
  void createGroup_missingPromotion_returnsBadRequest() {
    var response = createGroup(token("ADMIN"), Map.of("reference", "create-group-no-promotion"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createGroup_blankReference_returnsBadRequest() {
    var promotion = savePromotion("create-group-blank-promotion");

    var response =
        createGroup(
            token("ADMIN"), Map.of("reference", "  ", "promotionId", promotion.getId().toString()));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createGroup_onUnknownPromotion_returnsNotFound() {
    var response =
        createGroup(
            token("ADMIN"),
            Map.of(
                "reference", "create-group-unknown", "promotionId", UUID.randomUUID().toString()));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createGroup_asTeacher_returnsForbidden() {
    var promotion = savePromotion("create-group-teacher-promotion");

    var response =
        createGroup(
            token("TEACHER"),
            Map.of(
                "reference", "create-group-teacher", "promotionId", promotion.getId().toString()));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void createGroup_withoutToken_returnsUnauthorized() {
    var promotion = savePromotion("create-group-anon-promotion");

    var response =
        createGroup(
            null,
            Map.of("reference", "create-group-anon", "promotionId", promotion.getId().toString()));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void listGroups_returnsCreatedGroups() {
    saveGroup("list-groups-one");
    saveGroup("list-groups-two");

    var response = listGroups(token("ADMIN"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var references = response.getBody().stream().map(group -> group.get("reference")).toList();
    assertTrue(references.contains("list-groups-one"));
    assertTrue(references.contains("list-groups-two"));
  }

  @Test
  void listGroups_asTeacher_returnsOk() {
    saveGroup("list-groups-teacher");

    var response = listGroups(token("TEACHER"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void listGroups_asStudent_returnsForbidden() {
    var response = listGroupsAsString(token("STUDENT"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void listGroups_withoutToken_returnsUnauthorized() {
    var response = listGroupsAsString(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
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
    var promotion = savePromotion("Promotion 2025");
    return groupRepository.save(JGroup.builder().reference(reference).promotion(promotion).build());
  }

  private JPromotion savePromotion(String name) {
    return promotionRepository.save(JPromotion.builder().name(name).year(2025).build());
  }

  private ResponseEntity<Map> createGroup(String token, Object body) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/groups", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
  }

  private ResponseEntity<List<Map>> listGroups(String token) {
    return restTemplate.exchange(
        "/groups",
        HttpMethod.GET,
        new HttpEntity<>(headers(token)),
        new ParameterizedTypeReference<List<Map>>() {});
  }

  private ResponseEntity<String> listGroupsAsString(String token) {
    return restTemplate.exchange(
        "/groups", HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
  }

  private HttpHeaders headers(String token) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
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
