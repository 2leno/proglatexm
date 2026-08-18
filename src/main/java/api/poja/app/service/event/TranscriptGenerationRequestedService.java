package api.poja.app.service.event;

import api.poja.app.endpoint.event.model.TranscriptGenerationRequested;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.model.TranscriptStatus;
import api.poja.app.repository.JGradeRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.JTranscriptRepository;
import api.poja.app.repository.model.JGrade;
import api.poja.app.repository.model.JStudent;
import api.poja.app.repository.model.JTranscript;
import api.poja.app.service.TranscriptGenerator;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptGenerationRequestedService
    implements Consumer<TranscriptGenerationRequested> {

  private final JStudentRepository studentRepository;
  private final JGradeRepository gradeRepository;
  private final JTranscriptRepository transcriptRepository;
  private final BucketComponent bucketComponent;
  private final TranscriptGenerator transcriptGenerator;

  @Override
  public void accept(TranscriptGenerationRequested event) {
    var student = studentRepository.findById(event.getStudentId()).orElse(null);
    if (student == null) {
      log.warn("Transcript generation skipped: student {} not found", event.getStudentId());
      return;
    }
    var transcript = existingOrNew(student, event.getYear());
    try {
      var pdf =
          transcriptGenerator.generate(
              student, event.getYear(), gradesOf(student, event.getYear()));
      var key = bucketKey(student.getId(), event.getYear());
      bucketComponent.upload(toFile(pdf, transcript.getId()), key);
      transcript.setStatus(TranscriptStatus.GENERATED);
      transcript.setS3Key(key);
      transcript.setUpdatedAt(Instant.now());
    } catch (Exception e) {
      log.error("Transcript generation failed", e);
      transcript.setStatus(TranscriptStatus.FAILED);
      transcript.setUpdatedAt(Instant.now());
    }
    transcriptRepository.save(transcript);
  }

  private JTranscript existingOrNew(JStudent student, Integer year) {
    return transcriptRepository
        .findByStudentIdAndYear(student.getId(), year)
        .map(
            transcript -> {
              transcript.setStatus(TranscriptStatus.PENDING);
              transcript.setS3Key(null);
              transcript.setUpdatedAt(Instant.now());
              return transcript;
            })
        .orElseGet(
            () ->
                JTranscript.builder()
                    .student(student)
                    .year(year)
                    .status(TranscriptStatus.PENDING)
                    .s3Key(null)
                    .updatedAt(Instant.now())
                    .build());
  }

  private List<JGrade> gradesOf(JStudent student, Integer year) {
    return gradeRepository.findByStudentIdAndCurrentTrue(student.getId()).stream()
        .filter(grade -> grade.getExam().getSchedule().atZone(ZoneOffset.UTC).getYear() == year)
        .toList();
  }

  private String bucketKey(UUID studentId, Integer year) {
    return "transcripts/" + studentId + "/" + year + "/transcript.pdf";
  }

  @SneakyThrows
  private File toFile(byte[] pdf, UUID transcriptId) {
    var file = File.createTempFile("transcript-" + transcriptId, ".pdf");
    Files.write(file.toPath(), pdf);
    return file;
  }
}
