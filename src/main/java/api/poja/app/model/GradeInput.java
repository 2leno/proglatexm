package api.poja.app.model;

import lombok.Builder;

@Builder
public record GradeInput(String studentId, Double value) {}
