package api.poja.app.endpoint.rest.model.request;

import lombok.Builder;

@Builder
public record LoginRequest(String username, String password) {}
