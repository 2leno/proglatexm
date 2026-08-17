package api.poja.app.model;

import lombok.Builder;

@Builder
public record StudentInput(
    String firstName, String lastName, String reference, Parcours parcours) {}
