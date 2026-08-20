package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.request.GradeInput;
import api.poja.app.endpoint.rest.model.request.GradeModification;
import api.poja.app.endpoint.rest.model.response.Grade;
import api.poja.app.endpoint.rest.model.response.GradeHistoryEntry;
import api.poja.app.mapper.GradeMapper;
import api.poja.app.service.GradesService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GradeController {

  private final GradesService gradesService;
  private final GradeMapper gradeMapper;

  @PostMapping("/courses/{courseId}/exams/{examId}/grades")
  @ResponseStatus(HttpStatus.CREATED)
  public Grade recordGrade(
      @PathVariable UUID courseId,
      @PathVariable UUID examId,
      @RequestBody GradeInput input,
      Authentication authentication) {
    return gradesService.recordGrade(
        courseId, examId, gradeMapper.toDomain(input, examId), authentication);
  }

  @GetMapping("/students/{studentId}/grades")
  public List<Grade> getStudentGrades(
      @PathVariable UUID studentId,
      @RequestParam(required = false) UUID courseId,
      Authentication authentication) {
    return gradesService.getStudentGrades(studentId, courseId, authentication);
  }

  @GetMapping("/students/{studentId}/grades/{gradeId}/history")
  public List<GradeHistoryEntry> getGradeHistory(
      @PathVariable UUID studentId, @PathVariable UUID gradeId, Authentication authentication) {
    return gradesService.getGradeHistory(studentId, gradeId, authentication);
  }

  @PutMapping("/grades/{gradeId}")
  public Grade modifyGrade(
      @PathVariable UUID gradeId,
      @RequestBody GradeModification input,
      Authentication authentication) {
    return gradesService.modifyGrade(gradeId, input.newValue(), input.reason(), authentication);
  }
}
