package com.librasConnect.system.services;

import java.util.List;

import com.librasConnect.system.signs.PreparedStoredSeries;

public interface RecognitionLexiconCache {

    LexiconSnapshot snapshot();

    void invalidate();

    void registerSample(String sampleId);

    record LexiconSnapshot(List<CachedSample> samples) {
        public boolean isEmpty() {
            return samples.isEmpty();
        }
    }

    record CachedSample(String signId, String label, boolean bimanual, PreparedStoredSeries series) {
    }
}
