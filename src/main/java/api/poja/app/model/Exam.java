package api.poja.app.model;

import java.time.Instant;

public record Exam(String id, String name, Instant schedule, Double coefficient) {}
