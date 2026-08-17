package api.poja.app.model;

import lombok.Builder;

@Builder
public record Grade(String id, String courseId, String examId, Double value, Boolean current) {}
