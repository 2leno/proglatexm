package api.poja.app.service;

import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.endpoint.event.model.PojaEvent;
import api.poja.app.endpoint.event.model.TranscriptGenerationRequested;
import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.endpoint.rest.model.response.TranscriptBatch;
import api.poja.app.exception.ApiException;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.mapper.TranscriptMapper;
import api.poja.app.model.Transcript;
import api.poja.app.model.TranscriptStatus;
import api.poja.app.repository.JPromotionRepository;
import api.poja.app.repository.JStudentGroupPeriodRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.JTranscriptRepository;
import api.poja.app.repository.model.JStudent;
import api.poja.app.repository.model.JTranscript;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class TranscriptsService {

  private final JStudentRepository studentRepository;
  private final JTranscriptRepository transcriptRepository;
  private final JPromotionRepository promotionRepository;
  private final JStudentGroupPeriodRepository periodRepository;
  private final BucketComponent bucketComponent;
  private final StudentAccessGuard accessGuard;
  private final TranscriptMapper transcriptMapper;
  private final EventProducer<PojaEvent> eventProducer;

  @Transactional
  public Transcript generate(UUID studentId, Integer year, Authentication authentication) {
    accessGuard.ensureCanReadStudent(studentId, authentication);
    if (year == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Year is required");
    }
    requireStudent(studentId);
    var saved = transcriptRepository.save(pendingTranscript(studentId, year));
    eventProducer.accept(
        List.of(TranscriptGenerationRequested.builder().studentId(studentId).year(year).build()));
    return transcriptMapper.toDomain(saved);
  }

  @Transactional
  public Transcript send(UUID studentId, Integer year) {
    var transcript =
        transcriptRepository
            .findByStudentIdAndYear(studentId, year)
            .orElseThrow(() -> notFound("Transcript not found for this year"));
    if (transcript.getStatus() != TranscriptStatus.GENERATED) {
      throw notFound("Transcript not found for this year");
    }
    eventProducer.accept(
        List.of(TranscriptSendRequested.builder().studentId(studentId).year(year).build()));
    return transcriptMapper.toDomain(transcript);
  }

  @Transactional
  public TranscriptBatch sendAll(UUID promotionId, Integer year) {
    if (!promotionRepository.existsById(promotionId)) {
      throw notFound("Promotion not found");
    }
    var studentIds =
        periodRepository.findByGroupPromotionId(promotionId).stream()
            .map(period -> period.getStudent().getId())
            .distinct()
            .collect(Collectors.toList());
    if (studentIds.isEmpty()) {
      return TranscriptBatch.builder()
          .batchId(UUID.randomUUID().toString())
          .studentsQueued(0)
          .build();
    }
    var queuedStudentIds =
        transcriptRepository.findByStudentIdIn(studentIds).stream()
            .filter(t -> t.getYear().equals(year) && t.getStatus() == TranscriptStatus.GENERATED)
            .map(t -> t.getStudent().getId())
            .toList();
    eventProducer.accept(
        queuedStudentIds.stream()
            .map(
                id ->
                    (PojaEvent) TranscriptSendRequested.builder().studentId(id).year(year).build())
            .toList());
    return TranscriptBatch.builder()
        .batchId(UUID.randomUUID().toString())
        .studentsQueued(queuedStudentIds.size())
        .build();
  }

  @SneakyThrows
  @Transactional(readOnly = true)
  public byte[] download(UUID studentId, Integer year, Authentication authentication) {
    accessGuard.ensureCanReadStudent(studentId, authentication);
    var transcript = requireGenerated(studentId, year);
    var file = bucketComponent.download(transcript.getS3Key());
    return Files.readAllBytes(file.toPath());
  }

  @Transactional(readOnly = true)
  public String downloadUrl(UUID studentId, Integer year, Authentication authentication) {
    accessGuard.ensureCanReadStudent(studentId, authentication);
    var transcript = requireGenerated(studentId, year);
    return bucketComponent.presign(transcript.getS3Key(), Duration.ofMinutes(60)).toString();
  }

  @Transactional(readOnly = true)
  public Transcript getStatus(UUID studentId, Integer year, Authentication authentication) {
    accessGuard.ensureCanReadStudent(studentId, authentication);
    var transcript =
        transcriptRepository
            .findByStudentIdAndYear(studentId, year)
            .orElseThrow(() -> notFound("Transcript not found"));
    return transcriptMapper.toDomain(transcript);
  }

  private JTranscript requireGenerated(UUID studentId, Integer year) {
    var transcript =
        transcriptRepository
            .findByStudentIdAndYear(studentId, year)
            .orElseThrow(() -> notFound("Transcript not found"));
    if (transcript.getStatus() != TranscriptStatus.GENERATED) {
      throw notFound("Transcript not found");
    }
    return transcript;
  }

  private JTranscript pendingTranscript(UUID studentId, Integer year) {
    var existing = transcriptRepository.findByStudentIdAndYear(studentId, year);
    if (existing.isPresent()) {
      var transcript = existing.get();
      transcript.setStatus(TranscriptStatus.PENDING);
      transcript.setS3Key(null);
      transcript.setUpdatedAt(Instant.now());
      return transcript;
    }
    return JTranscript.builder()
        .student(requireStudent(studentId))
        .year(year)
        .status(TranscriptStatus.PENDING)
        .s3Key(null)
        .updatedAt(Instant.now())
        .build();
  }

  private JStudent requireStudent(UUID studentId) {
    return studentRepository.findById(studentId).orElseThrow(() -> notFound("Student not found"));
  }

  private ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, message);
  }
}
