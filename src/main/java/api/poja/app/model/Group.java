package api.poja.app.model;

import lombok.Builder;

@Builder
public record Group(String id, String reference, String promotionId) {}
