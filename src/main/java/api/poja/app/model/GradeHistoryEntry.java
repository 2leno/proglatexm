package api.poja.app.model;

import java.time.Instant;

public record GradeHistoryEntry(
    Double value, String reason, String modifiedBy, Instant modifiedAt) {}
