package api.poja.app.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "grade_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JGradeHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "grade_id", nullable = false)
  private JGrade grade;

  @Column(nullable = false)
  private Double value;

  @Column(nullable = false)
  private String reason;

  @Column(name = "modified_by", nullable = false)
  private String modifiedBy;

  @Column(name = "modified_at", nullable = false)
  private Instant modifiedAt;
}
