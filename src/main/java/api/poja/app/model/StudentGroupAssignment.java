package api.poja.app.model;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record StudentGroupAssignment(String groupId, LocalDate effectiveDate) {}
