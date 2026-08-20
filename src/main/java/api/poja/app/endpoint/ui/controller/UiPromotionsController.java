package api.poja.app.endpoint.ui.controller;

import api.poja.app.service.GraduateExcelGenerator;
import api.poja.app.service.GraduatesService;
import api.poja.app.service.UiBrowseService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@AllArgsConstructor
public class UiPromotionsController {

  private final UiBrowseService browseService;
  private final GraduatesService graduatesService;
  private final GraduateExcelGenerator excelGenerator;

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

  @GetMapping("/ui/promotions/{promotionId}/graduates")
  public String graduates(@PathVariable UUID promotionId, Model model) {
    model.addAttribute("promotion", browseService.promotionDetail(promotionId));
    model.addAttribute("graduates", graduatesService.computeGraduates(promotionId));
    return "ui/graduates";
  }

  @GetMapping("/ui/promotions/{promotionId}/graduates/download")
  public ResponseEntity<byte[]> graduatesDownload(@PathVariable UUID promotionId) {
    var graduates = graduatesService.computeGraduates(promotionId);
    var bytes = excelGenerator.generate(graduates);
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=graduates.xlsx")
        .body(bytes);
  }

  @GetMapping("/ui/students/{studentId}")
  public String student(@PathVariable UUID studentId, Model model) {
    model.addAttribute("student", browseService.studentDetail(studentId));
    return "ui/student";
  }
}
