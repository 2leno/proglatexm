package api.poja.app.repository;

import api.poja.app.repository.model.JTeacher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JTeacherRepository extends JpaRepository<JTeacher, UUID> {
  Optional<JTeacher> findByUsername(String username);
}
