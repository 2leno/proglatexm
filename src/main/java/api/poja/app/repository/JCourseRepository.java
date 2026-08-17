package api.poja.app.repository;

import api.poja.app.model.Parcours;
import api.poja.app.repository.model.JCourse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JCourseRepository extends JpaRepository<JCourse, UUID> {
  List<JCourse> findAllByParcours(Parcours parcours);
}
