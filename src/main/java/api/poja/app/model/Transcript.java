package api.poja.app.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Transcript(
    UUID id,
    UUID studentId,
    Integer year,
    TranscriptStatus status,
    String s3Key,
    Instant updatedAt) {}
