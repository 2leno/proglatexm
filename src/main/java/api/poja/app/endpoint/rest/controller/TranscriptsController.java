package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.model.response.TranscriptBatch;
import api.poja.app.endpoint.rest.model.response.TranscriptDownload;
import api.poja.app.endpoint.rest.model.response.TranscriptStatus;
import api.poja.app.mapper.TranscriptMapper;
import api.poja.app.service.TranscriptsService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TranscriptsController {

  private final TranscriptsService transcriptsService;
  private final TranscriptMapper transcriptMapper;

  @PostMapping("/students/{studentId}/transcripts/generate")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public TranscriptStatus generate(
      @PathVariable UUID studentId, @RequestParam Integer year, Authentication authentication) {
    return transcriptMapper.toRest(transcriptsService.generate(studentId, year, authentication));
  }

  @PostMapping("/students/{studentId}/transcripts/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public TranscriptStatus send(@PathVariable UUID studentId, @RequestParam Integer year) {
    return transcriptMapper.toRest(transcriptsService.send(studentId, year));
  }

  @PostMapping("/promotions/{promotionId}/transcripts/send-all")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public TranscriptBatch sendAll(@PathVariable UUID promotionId, @RequestParam Integer year) {
    return transcriptsService.sendAll(promotionId, year);
  }

  @GetMapping("/students/{studentId}/transcripts/{year}")
  public ResponseEntity<?> download(
      @PathVariable UUID studentId,
      @PathVariable Integer year,
      @RequestHeader(name = HttpHeaders.ACCEPT, required = false) String accept,
      Authentication authentication) {
    if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
      var downloadUrl = transcriptsService.downloadUrl(studentId, year, authentication);
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(TranscriptDownload.builder().downloadUrl(downloadUrl).build());
    }
    var pdf = transcriptsService.download(studentId, year, authentication);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
  }

  @GetMapping("/students/{studentId}/transcripts/{year}/status")
  public TranscriptStatus getStatus(
      @PathVariable UUID studentId, @PathVariable Integer year, Authentication authentication) {
    return transcriptMapper.toRest(transcriptsService.getStatus(studentId, year, authentication));
  }
}
