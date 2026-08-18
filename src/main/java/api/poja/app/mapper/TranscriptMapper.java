package api.poja.app.mapper;

import api.poja.app.endpoint.rest.model.response.TranscriptStatus;
import api.poja.app.model.Transcript;
import api.poja.app.repository.model.JTranscript;
import org.springframework.stereotype.Component;

@Component
public class TranscriptMapper {

  public Transcript toDomain(JTranscript transcript) {
    return Transcript.builder()
        .id(transcript.getId())
        .studentId(transcript.getStudent().getId())
        .year(transcript.getYear())
        .status(transcript.getStatus())
        .s3Key(transcript.getS3Key())
        .updatedAt(transcript.getUpdatedAt())
        .build();
  }

  public TranscriptStatus toRest(Transcript transcript) {
    return TranscriptStatus.builder()
        .studentId(transcript.studentId().toString())
        .year(transcript.year())
        .status(transcript.status())
        .s3Key(transcript.s3Key())
        .updatedAt(transcript.updatedAt())
        .build();
  }
}
