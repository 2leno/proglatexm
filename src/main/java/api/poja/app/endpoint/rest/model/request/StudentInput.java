package api.poja.app.endpoint.rest.model.request;

import api.poja.app.model.Parcours;
import lombok.Builder;

@Builder
public record StudentInput(
    String firstName,
    String lastName,
    String reference,
    Parcours parcours,
    String username,
    String password) {}
