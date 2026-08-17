package api.poja.app.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record Grade(UUID id, UUID studentId, UUID examId, Double value, Boolean current) {}
