package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.response.AnnualAverage;
import api.poja.app.endpoint.rest.model.response.GlobalAverage;
import api.poja.app.mapper.AverageMapper;
import api.poja.app.service.AveragesService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AveragesController {

  private final AveragesService averagesService;
  private final AverageMapper averageMapper;

  @GetMapping("/students/{studentId}/average")
  public AnnualAverage getAnnualAverage(
      @PathVariable UUID studentId,
      @RequestParam(required = false) Integer year,
      Authentication authentication) {
    return averageMapper.toRest(averagesService.getAnnualAverage(studentId, year, authentication));
  }

  @GetMapping("/students/{studentId}/average/global")
  public GlobalAverage getGlobalAverage(
      @PathVariable UUID studentId, Authentication authentication) {
    return averageMapper.toRest(averagesService.getGlobalAverage(studentId, authentication));
  }
}
