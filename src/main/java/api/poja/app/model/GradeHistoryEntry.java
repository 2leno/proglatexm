package api.poja.app.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record GradeHistoryEntry(
    Double value, String reason, String modifiedBy, Instant modifiedAt) {}
