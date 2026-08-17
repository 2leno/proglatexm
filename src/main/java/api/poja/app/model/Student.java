package api.poja.app.model;

public record Student(
    String id, String firstName, String lastName, String reference, Parcours parcours) {}
