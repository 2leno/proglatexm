package api.poja.app.service;

import api.poja.app.repository.model.JGrade;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GradeAverageComputer {

  public Double weightedAverage(List<JGrade> grades) {
    if (grades.isEmpty()) {
      return 0.0;
    }
    var totalCoefficient =
        grades.stream().mapToDouble(grade -> grade.getExam().getCoefficient()).sum();
    if (totalCoefficient == 0.0) {
      return 0.0;
    }
    var weightedSum =
        grades.stream()
            .mapToDouble(grade -> grade.getValue() * grade.getExam().getCoefficient())
            .sum();
    return weightedSum / totalCoefficient;
  }

  public List<Double> courseAverages(List<JGrade> grades) {
    return grades.stream()
        .collect(
            Collectors.groupingBy(
                grade -> grade.getExam().getCourse().getId(), Collectors.toList()))
        .values()
        .stream()
        .map(this::weightedAverage)
        .toList();
  }
}
