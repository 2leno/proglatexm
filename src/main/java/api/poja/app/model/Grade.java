package api.poja.app.model;

public record Grade(String id, String courseId, String examId, Double value, Boolean current) {}
