package api.poja.app.model;

import java.time.LocalDate;

public record StudentGroupPeriod(String reference, LocalDate startDate, LocalDate endDate) {}
