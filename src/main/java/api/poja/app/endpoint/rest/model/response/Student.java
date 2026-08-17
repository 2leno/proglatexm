package api.poja.app.endpoint.rest.model.response;

import api.poja.app.model.Parcours;
import lombok.Builder;

@Builder
public record Student(
    String id, String firstName, String lastName, String reference, Parcours parcours) {}
