package api.poja.app.repository;

import api.poja.app.repository.model.JGrade;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JGradeRepository extends JpaRepository<JGrade, UUID> {
  List<JGrade> findByStudentId(UUID studentId);

  List<JGrade> findByStudentIdAndCurrentTrue(UUID studentId);

  List<JGrade> findByStudentIdAndExamId(UUID studentId, UUID examId);
}
