package api.poja.app.endpoint.rest.model.response;

import lombok.Builder;

@Builder
public record GraduationFile(String fileKey, Integer graduatesCount) {}
