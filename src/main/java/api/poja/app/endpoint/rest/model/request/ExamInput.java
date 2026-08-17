package api.poja.app.endpoint.rest.model.request;

import java.time.Instant;
import lombok.Builder;

@Builder
public record ExamInput(String name, Instant schedule, Double coefficient) {}
