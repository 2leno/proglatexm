package api.poja.app.repository;

import api.poja.app.repository.model.JStudentGroupPeriod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JStudentGroupPeriodRepository extends JpaRepository<JStudentGroupPeriod, UUID> {
  List<JStudentGroupPeriod> findByStudentIdOrderByStartDateAsc(UUID studentId);

  Optional<JStudentGroupPeriod> findFirstByStudentIdOrderByStartDateDesc(UUID studentId);
}
