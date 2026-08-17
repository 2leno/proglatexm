package api.poja.app.endpoint.rest.model.response;

import lombok.Builder;

@Builder
public record Graduate(
    Integer rank, String reference, String lastName, String firstName, Double generalAverage) {}
