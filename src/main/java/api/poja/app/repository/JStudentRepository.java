package api.poja.app.repository;

import api.poja.app.repository.model.JStudent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JStudentRepository extends JpaRepository<JStudent, UUID> {
  Optional<JStudent> findByUsername(String username);

  boolean existsByStudentCode(String studentCode);
}
