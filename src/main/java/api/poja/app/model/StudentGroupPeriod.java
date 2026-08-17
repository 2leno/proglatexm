package api.poja.app.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentGroupPeriod(
    UUID id, UUID studentId, UUID groupId, LocalDate startDate, LocalDate endDate) {}
