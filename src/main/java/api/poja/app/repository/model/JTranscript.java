package api.poja.app.repository.model;

import api.poja.app.model.TranscriptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcript")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTranscript {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private JStudent student;

  @Column(nullable = false)
  private Integer year;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TranscriptStatus status;

  @Column(name = "s3_key")
  private String s3Key;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
