package api.poja.app.endpoint.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.model.Parcours;
import api.poja.app.repository.JAdminRepository;
import api.poja.app.repository.JCourseRepository;
import api.poja.app.repository.JExamRepository;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JGroupRepository;
import api.poja.app.repository.JPromotionRepository;
import api.poja.app.repository.JStudentGroupPeriodRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.JTeacherRepository;
import api.poja.app.repository.model.JAdmin;
import api.poja.app.repository.model.JCourse;
import api.poja.app.repository.model.JExam;
import api.poja.app.repository.model.JGrade;
import api.poja.app.repository.model.JGroup;
import api.poja.app.repository.model.JPromotion;
import api.poja.app.repository.model.JStudent;
import api.poja.app.repository.model.JStudentGroupPeriod;
import api.poja.app.repository.model.JTeacher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;

class UiIT extends FacadeIT {

  private static final String USERNAME = "ui-admin";
  private static final String PASSWORD = "ui-password";

  @MockBean EventProducer eventProducer;

  @Autowired TestRestTemplate restTemplate;
  @Autowired JAdminRepository adminRepository;
  @Autowired JTeacherRepository teacherRepository;
  @Autowired JStudentRepository studentRepository;
  @Autowired JPromotionRepository promotionRepository;
  @Autowired JGroupRepository groupRepository;
  @Autowired JStudentGroupPeriodRepository periodRepository;
  @Autowired JCourseRepository courseRepository;
  @Autowired JExamRepository examRepository;
  @Autowired JGradeRepository gradeRepository;
  @Autowired BCryptPasswordEncoder passwordEncoder;

  @BeforeEach
  void prepare() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    adminRepository
        .findByUsername(USERNAME)
        .orElseGet(
            () ->
                adminRepository.save(
                    JAdmin.builder()
                        .username(USERNAME)
                        .password(passwordEncoder.encode(PASSWORD))
                        .firstName("UI")
                        .lastName("Admin")
                        .build()));
  }

  @Test
  void login_redirectsToPromotions_andAllowsBrowsing() {
    var login = login(USERNAME, PASSWORD);

    assertEquals(HttpStatus.FOUND, login.getStatusCode());
    assertEquals("/ui/promotions", login.getHeaders().getLocation().getPath());

    var token = uiToken(login);
    var page = getWithToken("/ui/promotions", token);
    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertTrue(page.getBody().contains("Promotions"));
  }

  @Test
  void login_wrongCredentials_rerendersLoginWithError() {
    var login = login(USERNAME, "wrong-password");

    assertEquals(HttpStatus.OK, login.getStatusCode());
    assertTrue(login.getBody().contains("Invalid credentials"));
  }

  @Test
  void login_nonAdmin_rerendersLoginWithError() {
    teacherRepository
        .findByUsername("ui-teacher")
        .orElseGet(
            () ->
                teacherRepository.save(
                    JTeacher.builder()
                        .username("ui-teacher")
                        .password(passwordEncoder.encode("teacher-password"))
                        .reference("UI-TEACHER-1")
                        .firstName("UI")
                        .lastName("Teacher")
                        .build()));

    var login = login("ui-teacher", "teacher-password");

    assertEquals(HttpStatus.OK, login.getStatusCode());
    assertTrue(login.getBody().contains("ADMIN"));
  }

  @Test
  void browseWithoutCookie_redirectsToLogin() {
    var response = restTemplate.getForEntity("/ui/promotions", String.class);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertEquals("/ui/login", response.getHeaders().getLocation().getPath());
  }

  @Test
  void promotionPage_showsGroupsAndStudents() {
    var promotion =
        promotionRepository.save(JPromotion.builder().name("Promotion 2026 UI").year(2026).build());
    var group =
        groupRepository.save(JGroup.builder().reference("UI-GROUP-A").promotion(promotion).build());
    var student = saveStudent("ui-student-1");
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(group)
            .startDate(LocalDate.of(2026, 9, 1))
            .build());

    var token = loginAndGetToken();
    var page = getWithToken("/ui/promotions/" + promotion.getId(), token);

    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertTrue(page.getBody().contains("UI-GROUP-A"));
    assertTrue(page.getBody().contains("ui-student-1"));
  }

  @Test
  void studentPage_showsGradesAndAverages() {
    var student = saveStudent("ui-student-2");
    var course =
        courseRepository.save(
            JCourse.builder()
                .reference("UI-COURSE-1")
                .title("UI Mathematics")
                .credits(5)
                .parcours(Parcours.EL)
                .build());
    var exam =
        examRepository.save(
            JExam.builder()
                .course(course)
                .name("UI Midterm")
                .schedule(Instant.parse("2026-06-01T10:00:00Z"))
                .coefficient(1.0)
                .build());
    gradeRepository.save(
        JGrade.builder().student(student).exam(exam).value(12.0).current(true).build());

    var token = loginAndGetToken();
    var page = getWithToken("/ui/students/" + student.getId(), token);

    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertTrue(page.getBody().contains("Global average"));
    assertTrue(page.getBody().contains("12.00"));
  }

  @Test
  void graduatesPage_listsRankedGraduates() {
    var promotion =
        promotionRepository.save(JPromotion.builder().name("Grad Promo 2026").year(2026).build());
    var group =
        groupRepository.save(JGroup.builder().reference("GR-GROUP").promotion(promotion).build());
    var student = saveStudent("grad-student-1");
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(group)
            .startDate(LocalDate.of(2026, 9, 1))
            .build());
    var course =
        courseRepository.save(
            JCourse.builder()
                .reference("GR-COURSE-1")
                .title("GR Mathematics")
                .credits(5)
                .parcours(Parcours.EL)
                .build());
    var exam =
        examRepository.save(
            JExam.builder()
                .course(course)
                .name("GR Final")
                .schedule(Instant.parse("2026-06-01T10:00:00Z"))
                .coefficient(1.0)
                .build());
    gradeRepository.save(
        JGrade.builder().student(student).exam(exam).value(15.0).current(true).build());

    var token = loginAndGetToken();
    var page = getWithToken("/ui/promotions/" + promotion.getId() + "/graduates", token);

    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertTrue(page.getBody().contains("Graduates"));
    assertTrue(page.getBody().contains("grad-student-1"));
    assertTrue(page.getBody().contains("15.00"));
  }

  @Test
  void graduatesDownload_returnsXlsxFile() {
    var promotion =
        promotionRepository.save(JPromotion.builder().name("DL Promo 2026").year(2026).build());
    var group =
        groupRepository.save(JGroup.builder().reference("DL-GROUP").promotion(promotion).build());
    var student = saveStudent("dl-student-1");
    periodRepository.save(
        JStudentGroupPeriod.builder()
            .student(student)
            .group(group)
            .startDate(LocalDate.of(2026, 9, 1))
            .build());
    var course =
        courseRepository.save(
            JCourse.builder()
                .reference("DL-COURSE-1")
                .title("DL Math")
                .credits(5)
                .parcours(Parcours.EL)
                .build());
    var exam =
        examRepository.save(
            JExam.builder()
                .course(course)
                .name("DL Final")
                .schedule(Instant.parse("2026-06-01T10:00:00Z"))
                .coefficient(1.0)
                .build());
    gradeRepository.save(
        JGrade.builder().student(student).exam(exam).value(14.0).current(true).build());

    var token = loginAndGetToken();
    var response =
        restTemplate.exchange(
            "/ui/promotions/" + promotion.getId() + "/graduates/download",
            HttpMethod.GET,
            new HttpEntity<>(
                null,
                new HttpHeaders() {
                  {
                    add(HttpHeaders.COOKIE, "UI_TOKEN=" + token);
                  }
                }),
            byte[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getHeaders().getContentType().toString().contains("spreadsheetml"));
    assertTrue(response.getBody().length > 0);
  }

  @Test
  void graduatesPage_invalidPromotion_returnsNotFound() {
    var token = loginAndGetToken();
    var response =
        restTemplate.exchange(
            "/ui/promotions/00000000-0000-0000-0000-000000000000/graduates",
            HttpMethod.GET,
            new HttpEntity<>(
                null,
                new HttpHeaders() {
                  {
                    add(HttpHeaders.COOKIE, "UI_TOKEN=" + token);
                  }
                }),
            String.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  private String loginAndGetToken() {
    var login = login(USERNAME, PASSWORD);
    return uiToken(login);
  }

  private ResponseEntity<String> login(String username, String password) {
    var loginPage = restTemplate.getForEntity("/ui/login", String.class);
    assertEquals(
        HttpStatus.OK,
        loginPage.getStatusCode(),
        "login page: status="
            + loginPage.getStatusCode()
            + " location="
            + loginPage.getHeaders().getLocation()
            + " setCookie="
            + loginPage.getHeaders().get(HttpHeaders.SET_COOKIE));
    var xsrfCookie =
        loginPage.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
            .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
            .map(cookie -> cookie.split(";")[0].substring("XSRF-TOKEN=".length()))
            .findFirst()
            .orElseThrow();
    var xsrfField =
        Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(loginPage.getBody());
    assertTrue(xsrfField.find());
    var form = new LinkedMultiValueMap<String, String>();
    form.add("username", username);
    form.add("password", password);
    form.add("_csrf", xsrfField.group(1));
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + xsrfCookie);
    return restTemplate.postForEntity("/ui/login", new HttpEntity<>(form, headers), String.class);
  }

  private String uiToken(ResponseEntity<?> response) {
    return response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
        .filter(cookie -> cookie.startsWith("UI_TOKEN="))
        .map(cookie -> cookie.split(";")[0].substring("UI_TOKEN=".length()))
        .findFirst()
        .orElseThrow();
  }

  private ResponseEntity<String> getWithToken(String path, String token) {
    var headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, "UI_TOKEN=" + token);
    return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
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
}
