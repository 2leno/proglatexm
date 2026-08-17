package api.poja.app.endpoint.rest.model.response;

import lombok.Builder;

@Builder
public record AnnualAverage(Integer year, Double average, Integer credits) {}
