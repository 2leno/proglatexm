package api.poja.app.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record Student(
    UUID id,
    String username,
    String password,
    String firstName,
    String lastName,
    String reference,
    Parcours parcours) {}
