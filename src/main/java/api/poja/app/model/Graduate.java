package api.poja.app.model;

import lombok.Builder;

@Builder
public record Graduate(
    Integer rank, String reference, String lastName, String firstName, Double generalAverage) {}
