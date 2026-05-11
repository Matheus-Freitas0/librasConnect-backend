package com.librasConnect.system.dto.v1;

import java.time.Instant;

public record SampleMetaDto(String id, String signId, Instant createdAt, int durationMs, int frameCount) {
}
