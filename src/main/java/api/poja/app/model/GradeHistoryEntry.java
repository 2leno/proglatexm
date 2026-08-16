package api.poja.app.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeHistoryEntry {
  private Double value;
  private String reason;
  private String modifiedBy;
  private Instant modifiedAt;
}
