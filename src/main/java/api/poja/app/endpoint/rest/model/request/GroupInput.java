package api.poja.app.endpoint.rest.model.request;

import lombok.Builder;

@Builder
public record GroupInput(String reference, String promotionId) {}
