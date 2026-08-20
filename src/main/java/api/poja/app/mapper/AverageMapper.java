package api.poja.app.mapper;

import api.poja.app.model.AnnualAverage;
import api.poja.app.model.GlobalAverage;
import org.springframework.stereotype.Component;

@Component
public class AverageMapper {

  public api.poja.app.endpoint.rest.model.response.AnnualAverage toRest(AnnualAverage average) {
    return api.poja.app.endpoint.rest.model.response.AnnualAverage.builder()
        .year(average.year())
        .average(average.average())
        .credits(average.credits())
        .build();
  }

  public api.poja.app.endpoint.rest.model.response.GlobalAverage toRest(GlobalAverage average) {
    return api.poja.app.endpoint.rest.model.response.GlobalAverage.builder()
        .average(average.average())
        .build();
  }
}
