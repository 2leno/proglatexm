package api.poja.app.endpoint.rest.model.request;

import lombok.Builder;

@Builder
public record GradeInput(String studentId, Double value) {}
