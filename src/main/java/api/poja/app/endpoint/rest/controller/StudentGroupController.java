package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.request.StudentGroupAssignment;
import api.poja.app.endpoint.rest.model.response.StudentGroupPeriod;
import api.poja.app.mapper.StudentGroupPeriodMapper;
import api.poja.app.service.GroupsService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class StudentGroupController {

  private final GroupsService groupsService;
  private final StudentGroupPeriodMapper periodMapper;

  @PostMapping("/students/{studentId}/groups")
  @ResponseStatus(HttpStatus.CREATED)
  public StudentGroupPeriod assign(
      @PathVariable UUID studentId, @RequestBody StudentGroupAssignment input) {
    return groupsService.assign(studentId, periodMapper.toDomain(input, studentId));
  }

  @GetMapping("/students/{studentId}/groups/history")
  public List<StudentGroupPeriod> getHistory(
      @PathVariable UUID studentId, Authentication authentication) {
    return groupsService.getHistory(studentId, authentication);
  }
}
