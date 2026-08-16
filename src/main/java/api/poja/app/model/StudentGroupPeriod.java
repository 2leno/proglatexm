package api.poja.app.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGroupPeriod {
  private String groupCode;
  private LocalDate startDate;
  private LocalDate endDate;
}
