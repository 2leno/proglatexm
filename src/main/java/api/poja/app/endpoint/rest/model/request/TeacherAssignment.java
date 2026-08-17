package api.poja.app.endpoint.rest.model.request;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TeacherAssignment(List<UUID> teacherIds) {}
