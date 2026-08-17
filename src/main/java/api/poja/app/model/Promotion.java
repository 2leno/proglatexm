package api.poja.app.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record Promotion(UUID id, String name, Integer year) {}
