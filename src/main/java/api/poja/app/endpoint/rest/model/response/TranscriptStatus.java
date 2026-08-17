package api.poja.app.endpoint.rest.model.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record TranscriptStatus(
    String studentId,
    Integer year,
    api.poja.app.model.TranscriptStatus status,
    String s3Key,
    Instant updatedAt) {}
