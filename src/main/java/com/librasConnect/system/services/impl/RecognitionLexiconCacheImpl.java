package com.librasConnect.system.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.librasConnect.system.models.SignSample;
import com.librasConnect.system.repositories.SignSampleRepository;
import com.librasConnect.system.services.RecognitionLexiconCache;
import com.librasConnect.system.signs.ClipTemporalDistance;

@Service
public class RecognitionLexiconCacheImpl implements RecognitionLexiconCache {

    private final SignSampleRepository signSampleRepository;
    private final int dtwMaxSeriesPoints;
    private volatile LexiconSnapshot cached;

    public RecognitionLexiconCacheImpl(
            SignSampleRepository signSampleRepository,
            @Value("${app.recognizer.dtw-max-series-points:128}") int dtwMaxSeriesPoints) {
        this.signSampleRepository = signSampleRepository;
        this.dtwMaxSeriesPoints = Math.max(8, dtwMaxSeriesPoints);
    }

    @Override
    public LexiconSnapshot snapshot() {
        LexiconSnapshot current = cached;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cached == null) {
                cached = load();
            }
            return cached;
        }
    }

    @Override
    public void invalidate() {
        cached = null;
    }

    private LexiconSnapshot load() {
        List<CachedSample> samples = new ArrayList<>();
        for (SignSample sample : signSampleRepository.findAllWithSign()) {
            var sign = sample.getSign();
            samples.add(new CachedSample(
                    sign.getId(),
                    sign.getLabel(),
                    sign.isBimanual(),
                    ClipTemporalDistance.prepareStored(sample.getFrames(), sample.getDurationMs(), dtwMaxSeriesPoints)));
        }
        return new LexiconSnapshot(samples);
    }
}
