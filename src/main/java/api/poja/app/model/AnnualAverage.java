package api.poja.app.model;

import lombok.Builder;

@Builder
public record AnnualAverage(Integer year, Double average, Integer credits) {}
