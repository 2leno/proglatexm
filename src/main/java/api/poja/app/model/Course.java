package api.poja.app.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {
  private String id;
  private String reference;
  private String title;
  private Integer credits;
  private Parcours parcours;
  private List<String> teacherIds;
}
