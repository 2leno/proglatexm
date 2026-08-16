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
public class Transcript {
  private String studentId;
  private Integer year;
  private TranscriptStatus status;
  private String s3Key;
  private Instant updatedAt;
}
