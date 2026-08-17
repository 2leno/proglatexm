package api.poja.app.model;

import lombok.Builder;

@Builder
public record CourseInput(String reference, String title, Integer credits, Parcours parcours) {}
