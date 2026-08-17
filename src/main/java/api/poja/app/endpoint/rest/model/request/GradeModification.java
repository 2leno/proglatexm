package api.poja.app.endpoint.rest.model.request;

import lombok.Builder;

@Builder
public record GradeModification(Double newValue, String reason) {}
