package api.poja.app.mapper;

import api.poja.app.model.Promotion;
import api.poja.app.repository.model.JPromotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

  public Promotion toDomain(JPromotion promotion) {
    return Promotion.builder()
        .id(promotion.getId())
        .name(promotion.getName())
        .year(promotion.getYear())
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.Promotion toRest(Promotion promotion) {
    return api.poja.app.endpoint.rest.model.response.Promotion.builder()
        .id(promotion.id() == null ? null : promotion.id().toString())
        .name(promotion.name())
        .year(promotion.year())
        .build();
  }
}
