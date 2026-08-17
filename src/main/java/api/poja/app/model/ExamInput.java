package api.poja.app.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record ExamInput(String name, Instant schedule, Double coefficient) {}
