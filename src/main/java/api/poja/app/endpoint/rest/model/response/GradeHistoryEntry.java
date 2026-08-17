package api.poja.app.endpoint.rest.model.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record GradeHistoryEntry(
    Double value, String reason, String modifiedBy, Instant modifiedAt) {}
