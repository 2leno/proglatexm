package api.poja.app.endpoint.rest.model.response;

import lombok.Builder;

@Builder
public record Group(String id, String reference, String promotionId) {}
