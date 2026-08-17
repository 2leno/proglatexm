package api.poja.app.model;

import java.time.Instant;

public record Transcript(
    String studentId, Integer year, TranscriptStatus status, String s3Key, Instant updatedAt) {}
