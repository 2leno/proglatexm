package api.poja.app.model;

import java.time.Instant;

public record ExamInput(String name, Instant schedule, Double coefficient) {}
