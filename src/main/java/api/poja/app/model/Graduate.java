package api.poja.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Graduate {
  private Integer rank;
  private String studentCode;
  private String lastName;
  private String firstName;
  private Double generalAverage;
}
