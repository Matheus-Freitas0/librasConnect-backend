package com.librasConnect.system.signs;

import com.fasterxml.jackson.databind.JsonNode;
import com.librasConnect.system.dto.v1.ClipPayloadDto;

public final class ClipComparison {

    private ClipComparison() {
    }

    public static double temporalDistance(ClipPayloadDto query, JsonNode storedFrames, int storedDurationMs,
            int maxSeriesPoints) {
        return ClipTemporalDistance.dtwAverageCost(query, storedFrames, storedDurationMs, maxSeriesPoints);
    }
}
