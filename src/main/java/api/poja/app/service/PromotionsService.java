package api.poja.app.service;

import api.poja.app.mapper.PromotionMapper;
import api.poja.app.model.Promotion;
import api.poja.app.repository.JPromotionRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class PromotionsService {

  private final JPromotionRepository promotionRepository;
  private final PromotionMapper promotionMapper;

  @Transactional(readOnly = true)
  public List<Promotion> listPromotions() {
    return promotionRepository.findAll().stream().map(promotionMapper::toDomain).toList();
  }
}
