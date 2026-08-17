package api.poja.app.model;

import lombok.Builder;

@Builder
public record LoginResponse(String token, Role role) {}
