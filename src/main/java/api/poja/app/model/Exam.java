package api.poja.app.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Exam(UUID id, UUID courseId, String name, Instant schedule, Double coefficient) {}
