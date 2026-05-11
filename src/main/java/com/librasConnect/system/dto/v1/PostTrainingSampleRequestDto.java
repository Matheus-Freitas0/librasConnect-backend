package com.librasConnect.system.dto.v1;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostTrainingSampleRequestDto(
        @NotBlank @Size(min = 1, max = 60) String label,
        @Size(max = 280) String description,
        int durationMs,
        List<FrameDto> frames) {
}
