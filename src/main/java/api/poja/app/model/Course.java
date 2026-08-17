package api.poja.app.model;

import java.util.List;

public record Course(
    String id,
    String reference,
    String title,
    Integer credits,
    Parcours parcours,
    List<String> teacherIds) {}
