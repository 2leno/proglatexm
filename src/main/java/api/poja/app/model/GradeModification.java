package api.poja.app.model;

import lombok.Builder;

@Builder
public record GradeModification(Double newValue, String reason) {}
