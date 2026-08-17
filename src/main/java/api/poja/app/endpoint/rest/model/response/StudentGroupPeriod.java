package api.poja.app.endpoint.rest.model.response;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record StudentGroupPeriod(String reference, LocalDate startDate, LocalDate endDate) {}
