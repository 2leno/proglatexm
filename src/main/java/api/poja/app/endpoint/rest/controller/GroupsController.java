package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.request.GroupInput;
import api.poja.app.endpoint.rest.model.response.Group;
import api.poja.app.mapper.GroupMapper;
import api.poja.app.service.GroupsService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GroupsController {

  private final GroupsService groupsService;
  private final GroupMapper groupMapper;

  @GetMapping("/groups")
  public List<Group> listGroups() {
    return groupsService.listGroups().stream().map(groupMapper::toRest).toList();
  }

  @PostMapping("/groups")
  @ResponseStatus(HttpStatus.CREATED)
  public Group createGroup(@RequestBody GroupInput input) {
    return groupMapper.toRest(groupsService.createGroup(groupMapper.toDomain(input)));
  }
}
