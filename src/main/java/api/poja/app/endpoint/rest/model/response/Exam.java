package api.poja.app.endpoint.rest.model.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record Exam(String id, String name, Instant schedule, Double coefficient) {}
