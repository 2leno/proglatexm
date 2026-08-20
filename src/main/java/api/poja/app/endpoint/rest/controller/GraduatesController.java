package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.response.Graduate;
import api.poja.app.endpoint.rest.model.response.GraduationFile;
import api.poja.app.mapper.GraduateMapper;
import api.poja.app.service.GraduatesService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GraduatesController {

  private final GraduatesService graduatesService;
  private final GraduateMapper graduateMapper;

  @PostMapping("/promotions/{promotionId}/graduates/generate")
  public GraduationFile generate(@PathVariable UUID promotionId) {
    return graduatesService.generate(promotionId);
  }

  @GetMapping("/promotions/{promotionId}/graduates/download")
  public ResponseEntity<byte[]> download(@PathVariable UUID promotionId) {
    var file = graduatesService.download(promotionId);
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(file);
  }

  @GetMapping("/promotions/{promotionId}/graduates")
  public List<Graduate> listGraduates(@PathVariable UUID promotionId) {
    return graduatesService.computeGraduates(promotionId).stream()
        .map(graduateMapper::toRest)
        .toList();
  }
}
