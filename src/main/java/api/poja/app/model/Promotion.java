package api.poja.app.model;

import lombok.Builder;

@Builder
public record Promotion(String id, String name, Integer year) {}
