package api.poja.app.model;

import lombok.Builder;

@Builder
public record LoginRequest(String username, String password) {}
