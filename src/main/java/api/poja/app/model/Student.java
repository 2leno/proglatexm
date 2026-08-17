package api.poja.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
  private String id;
  private String firstName;
  private String lastName;
  private String studentCode;
  private Parcours parcours;
}
