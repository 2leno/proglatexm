package api.poja.app.endpoint.rest.model.response;

import lombok.Builder;

@Builder
public record Grade(String id, String courseId, String examId, Double value, Boolean current) {}
