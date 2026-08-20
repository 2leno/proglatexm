package api.poja.app.endpoint.rest.model.response;

import lombok.Builder;

@Builder
public record TranscriptBatch(String batchId, Integer studentsQueued) {}
