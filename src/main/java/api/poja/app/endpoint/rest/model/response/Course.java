package api.poja.app.endpoint.rest.model.response;

import api.poja.app.model.Parcours;
import java.util.List;
import lombok.Builder;

@Builder
public record Course(
    String id,
    String reference,
    String title,
    Integer credits,
    Parcours parcours,
    List<String> teacherIds) {}
