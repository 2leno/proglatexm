package api.poja.app.repository.model;

import api.poja.app.model.Parcours;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JCourse {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String reference;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private Integer credits;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Parcours parcours;

  @ManyToMany
  @JoinTable(
      name = "course_teacher",
      joinColumns = @JoinColumn(name = "course_id"),
      inverseJoinColumns = @JoinColumn(name = "teacher_id"))
  @Builder.Default
  private List<JTeacher> teachers = new ArrayList<>();
}
