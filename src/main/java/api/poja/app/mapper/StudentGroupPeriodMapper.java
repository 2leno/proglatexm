package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.request.StudentGroupAssignment;
import api.poja.app.exception.ApiException;
import api.poja.app.model.StudentGroupPeriod;
import api.poja.app.repository.model.JStudentGroupPeriod;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StudentGroupPeriodMapper {

  public StudentGroupPeriod toDomain(StudentGroupAssignment input, UUID studentId) {
    return StudentGroupPeriod.builder()
        .studentId(studentId)
        .groupId(parseUuid(input.groupId()))
        .startDate(input.effectiveDate())
        .build();
  }

  public StudentGroupPeriod toDomain(JStudentGroupPeriod period) {
    return StudentGroupPeriod.builder()
        .id(period.getId())
        .studentId(period.getStudent().getId())
        .groupId(period.getGroup().getId())
        .startDate(period.getStartDate())
        .endDate(period.getEndDate())
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.StudentGroupPeriod toRest(
      StudentGroupPeriod period, String groupReference) {
    return api.poja.app.endpoint.rest.model.response.StudentGroupPeriod.builder()
        .reference(groupReference)
        .startDate(period.startDate())
        .endDate(period.endDate())
        .build();
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid group id");
    }
  }
}
