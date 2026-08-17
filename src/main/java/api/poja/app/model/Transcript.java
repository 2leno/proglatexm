package api.poja.app.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record Transcript(
    String studentId, Integer year, TranscriptStatus status, String s3Key, Instant updatedAt) {}
