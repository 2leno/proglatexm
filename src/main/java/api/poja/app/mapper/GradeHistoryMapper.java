package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.response.GradeHistoryEntry;
import api.poja.app.model.GradeHistory;
import api.poja.app.repository.model.JGradeHistory;
import org.springframework.stereotype.Component;

@Component
public class GradeHistoryMapper {

  public GradeHistory toDomain(JGradeHistory history) {
    return GradeHistory.builder()
        .id(history.getId())
        .gradeId(history.getGrade().getId())
        .value(history.getValue())
        .reason(history.getReason())
        .modifiedBy(history.getModifiedBy())
        .modifiedAt(history.getModifiedAt())
        .build();
  }

  public GradeHistoryEntry toRest(GradeHistory history) {
    return GradeHistoryEntry.builder()
        .value(history.value())
        .reason(history.reason())
        .modifiedBy(history.modifiedBy())
        .modifiedAt(history.modifiedAt())
        .build();
  }
}
