package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.response.Promotion;
import api.poja.app.mapper.PromotionMapper;
import api.poja.app.service.PromotionsService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PromotionsController {

  private final PromotionsService promotionsService;
  private final PromotionMapper promotionMapper;

  @GetMapping("/promotions")
  public List<Promotion> listPromotions() {
    return promotionsService.listPromotions().stream().map(promotionMapper::toRest).toList();
  }
}
