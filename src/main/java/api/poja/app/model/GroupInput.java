package api.poja.app.model;

import lombok.Builder;

@Builder
public record GroupInput(String reference, String promotionId) {}
