package api.poja.app.model;

import lombok.Builder;

@Builder
public record Student(
    String id, String firstName, String lastName, String reference, Parcours parcours) {}
