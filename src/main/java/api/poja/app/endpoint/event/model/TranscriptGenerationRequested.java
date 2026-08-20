package api.poja.app.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class TranscriptGenerationRequested extends PojaEvent {
  @JsonProperty("studentId")
  private UUID studentId;

  @JsonProperty("year")
  private Integer year;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
