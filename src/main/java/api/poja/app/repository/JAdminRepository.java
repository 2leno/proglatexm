package api.poja.app.repository;

import api.poja.app.repository.model.JAdmin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JAdminRepository extends JpaRepository<JAdmin, UUID> {
  Optional<JAdmin> findByUsername(String username);
}
