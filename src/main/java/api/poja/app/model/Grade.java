package api.poja.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grade {
  private String id;
  private String courseId;
  private String examId;
  private Double value;
  private Boolean current;
}
