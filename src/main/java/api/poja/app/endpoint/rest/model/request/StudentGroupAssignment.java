package api.poja.app.endpoint.rest.model.request;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record StudentGroupAssignment(String groupId, LocalDate effectiveDate) {}
