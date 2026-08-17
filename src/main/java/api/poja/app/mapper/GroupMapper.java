package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.request.GroupInput;
import api.poja.app.model.Group;
import api.poja.app.repository.model.JGroup;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

  public Group toDomain(JGroup group) {
    return Group.builder()
        .id(group.getId())
        .reference(group.getReference())
        .promotionId(group.getPromotion().getId())
        .build();
  }

  public Group toDomain(GroupInput input) {
    return Group.builder()
        .reference(input.reference())
        .promotionId(input.promotionId() == null ? null : UUID.fromString(input.promotionId()))
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.Group toRest(Group group) {
    return api.poja.app.endpoint.rest.model.response.Group.builder()
        .id(group.id() == null ? null : group.id().toString())
        .reference(group.reference())
        .promotionId(group.promotionId() == null ? null : group.promotionId().toString())
        .build();
  }
}
