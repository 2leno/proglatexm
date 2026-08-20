package api.poja.app.mapper;

import org.springframework.stereotype.Component;

@Component
public class GraduateMapper {

  public api.poja.app.endpoint.rest.model.response.Graduate toRest(
      api.poja.app.model.Graduate graduate) {
    return api.poja.app.endpoint.rest.model.response.Graduate.builder()
        .rank(graduate.rank())
        .reference(graduate.reference())
        .lastName(graduate.lastName())
        .firstName(graduate.firstName())
        .generalAverage(graduate.generalAverage())
        .build();
  }
}
