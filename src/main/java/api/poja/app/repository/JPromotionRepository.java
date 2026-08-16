package api.poja.app.repository;

import api.poja.app.repository.model.JPromotion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JPromotionRepository extends JpaRepository<JPromotion, UUID> {}
