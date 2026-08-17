package api.poja.app.model;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record StudentGroupPeriod(String reference, LocalDate startDate, LocalDate endDate) {}
