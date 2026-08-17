package api.poja.app.endpoint.rest.model.request;

import api.poja.app.model.Parcours;
import lombok.Builder;

@Builder
public record CourseInput(String reference, String title, Integer credits, Parcours parcours) {}
