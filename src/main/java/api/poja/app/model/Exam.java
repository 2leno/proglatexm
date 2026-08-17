package api.poja.app.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record Exam(String id, String name, Instant schedule, Double coefficient) {}
