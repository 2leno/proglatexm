package api.poja.app.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record GradeHistory(
    UUID id, UUID gradeId, Double value, String reason, String modifiedBy, Instant modifiedAt) {}
