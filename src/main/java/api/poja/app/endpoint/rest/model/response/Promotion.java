package api.poja.app.endpoint.rest.model.response;

import lombok.Builder;

@Builder
public record Promotion(String id, String name, Integer year) {}
