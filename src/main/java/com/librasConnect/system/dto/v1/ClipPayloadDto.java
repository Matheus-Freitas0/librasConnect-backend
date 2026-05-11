package com.librasConnect.system.dto.v1;

import java.util.List;

public record ClipPayloadDto(int durationMs, List<FrameDto> frames) {
}
