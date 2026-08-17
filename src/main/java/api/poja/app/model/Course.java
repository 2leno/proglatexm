package api.poja.app.model;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Course(
    UUID id,
    String reference,
    String title,
    Integer credits,
    Parcours parcours,
    List<UUID> teacherIds) {}
