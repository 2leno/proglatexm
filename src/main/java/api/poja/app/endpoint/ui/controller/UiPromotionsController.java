package api.poja.app.endpoint.ui.controller;

import api.poja.app.service.UiBrowseService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@AllArgsConstructor
public class UiPromotionsController {

  private final UiBrowseService browseService;

  @GetMapping("/ui/promotions")
  public String promotions(Model model) {
    model.addAttribute("promotions", browseService.listPromotions());
    return "ui/promotions";
  }

  @GetMapping("/ui/promotions/{promotionId}")
  public String promotion(@PathVariable UUID promotionId, Model model) {
    model.addAttribute("promotion", browseService.promotionDetail(promotionId));
    return "ui/promotion";
  }

  @GetMapping("/ui/students/{studentId}")
  public String student(@PathVariable UUID studentId, Model model) {
    model.addAttribute("student", browseService.studentDetail(studentId));
    return "ui/student";
  }
}
