package com.librasConnect.system.signs;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record PreparedStoredSeries(List<StoredTimedFrame> frames) {

    public record StoredTimedFrame(int t, JsonNode frame) {
    }
}
