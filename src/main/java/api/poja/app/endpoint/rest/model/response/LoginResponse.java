package api.poja.app.endpoint.rest.model.response;

import api.poja.app.model.Role;
import lombok.Builder;

@Builder
public record LoginResponse(String token, Role role) {}
