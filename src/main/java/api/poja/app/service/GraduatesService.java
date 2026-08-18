package api.poja.app.service;

import api.poja.app.endpoint.rest.model.response.GraduationFile;
import api.poja.app.exception.ApiException;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.model.Graduate;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JPromotionRepository;
import api.poja.app.repository.JStudentGroupPeriodRepository;
import java.io.File;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class GraduatesService {

  private final JPromotionRepository promotionRepository;
  private final JStudentGroupPeriodRepository periodRepository;
  private final JGradeRepository gradeRepository;
  private final BucketComponent bucketComponent;
  private final GradeAverageComputer gradeAverageComputer;
  private final GraduateExcelGenerator excelGenerator;

  @SneakyThrows
  @Transactional(readOnly = true)
  public GraduationFile generate(UUID promotionId) {
    requirePromotion(promotionId);
    var graduates = computeGraduates(promotionId);
    var file = File.createTempFile("graduates-" + promotionId, ".xlsx");
    Files.write(file.toPath(), excelGenerator.generate(graduates));
    var key = bucketKey(promotionId);
    bucketComponent.upload(file, key);
    return GraduationFile.builder().fileKey(key).graduatesCount(graduates.size()).build();
  }

  @Transactional(readOnly = true)
  public List<Graduate> computeGraduates(UUID promotionId) {
    requirePromotion(promotionId);
    var studentIds =
        periodRepository.findByGroupPromotionId(promotionId).stream()
            .map(period -> period.getStudent().getId())
            .distinct()
            .toList();
    var graduates =
        studentIds.stream()
            .map(this::graduateIfValid)
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(Graduate::generalAverage).reversed())
            .toList();
    return rank(graduates);
  }

  @SneakyThrows
  @Transactional(readOnly = true)
  public byte[] download(UUID promotionId) {
    requirePromotion(promotionId);
    try {
      var file = bucketComponent.download(bucketKey(promotionId));
      return Files.readAllBytes(file.toPath());
    } catch (Exception e) {
      throw new ApiException(HttpStatus.NOT_FOUND, "No file generated yet for this promotion");
    }
  }

  private Optional<Graduate> graduateIfValid(UUID studentId) {
    var grades = gradeRepository.findByStudentIdAndCurrentTrue(studentId);
    if (grades.isEmpty()) {
      return Optional.empty();
    }
    var allCoursesPassed =
        gradeAverageComputer.courseAverages(grades).stream().noneMatch(average -> average < 10.0);
    if (!allCoursesPassed) {
      return Optional.empty();
    }
    var student = grades.get(0).getStudent();
    return Optional.of(
        Graduate.builder()
            .reference(student.getReference())
            .lastName(student.getLastName())
            .firstName(student.getFirstName())
            .generalAverage(gradeAverageComputer.weightedAverage(grades))
            .build());
  }

  private List<Graduate> rank(List<Graduate> graduates) {
    return IntStream.range(0, graduates.size())
        .mapToObj(i -> toRanked(graduates.get(i), i))
        .toList();
  }

  private Graduate toRanked(Graduate graduate, int listIndex) {
    return Graduate.builder()
        .rank(listIndex + 1)
        .reference(graduate.reference())
        .lastName(graduate.lastName())
        .firstName(graduate.firstName())
        .generalAverage(graduate.generalAverage())
        .build();
  }

  private void requirePromotion(UUID promotionId) {
    if (!promotionRepository.existsById(promotionId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Promotion not found");
    }
  }

  private String bucketKey(UUID promotionId) {
    return "graduates/" + promotionId + "/graduates.xlsx";
  }
}
