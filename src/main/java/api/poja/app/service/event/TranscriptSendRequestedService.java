package api.poja.app.service.event;

import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.mail.Email;
import api.poja.app.mail.Mailer;
import api.poja.app.model.TranscriptStatus;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.JTranscriptRepository;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptSendRequestedService implements Consumer<TranscriptSendRequested> {

  private final JStudentRepository studentRepository;
  private final JTranscriptRepository transcriptRepository;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @Override
  public void accept(TranscriptSendRequested event) {
    var student = studentRepository.findById(event.getStudentId()).orElse(null);
    if (student == null) {
      log.warn("Transcript send skipped: student {} not found", event.getStudentId());
      return;
    }
    var transcript =
        transcriptRepository.findByStudentIdAndYear(student.getId(), event.getYear()).orElse(null);
    if (transcript == null || transcript.getStatus() != TranscriptStatus.GENERATED) {
      log.warn(
          "Transcript send skipped: no generated transcript for student {} year {}",
          event.getStudentId(),
          event.getYear());
      return;
    }
    try {
      var attachment = bucketComponent.download(transcript.getS3Key());
      mailer.accept(
          new Email(
              new InternetAddress(student.getEmail()),
              List.of(),
              List.of(),
              "Academic transcript - Year " + event.getYear(),
              "Please find attached the academic transcript for year " + event.getYear() + ".",
              List.of(attachment)));
      transcript.setStatus(TranscriptStatus.SENT);
    } catch (Exception e) {
      log.error("Transcript send failed", e);
      transcript.setStatus(TranscriptStatus.FAILED);
    }
    transcript.setUpdatedAt(Instant.now());
    transcriptRepository.save(transcript);
  }
}
