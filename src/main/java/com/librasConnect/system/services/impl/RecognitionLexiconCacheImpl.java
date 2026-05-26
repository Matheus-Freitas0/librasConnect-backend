package com.librasConnect.system.services.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.FrameDto;
import com.librasConnect.system.models.SignSample;
import com.librasConnect.system.repositories.SignSampleRepository;
import com.librasConnect.system.services.RecognitionLexiconCache;
import com.librasConnect.system.signs.ClipSegmentTailTrim;
import com.librasConnect.system.signs.ClipTemporalDistance;
import com.librasConnect.system.signs.PreparedStoredSeries;

@Service
public class RecognitionLexiconCacheImpl implements RecognitionLexiconCache {

    private static final TypeReference<List<FrameDto>> FRAME_LIST = new TypeReference<>() {
    };

    private final SignSampleRepository signSampleRepository;
    private final ObjectMapper objectMapper;
    private final int dtwMaxSeriesPoints;
    private volatile LexiconSnapshot cached;

    public RecognitionLexiconCacheImpl(
            SignSampleRepository signSampleRepository,
            ObjectMapper objectMapper,
            @Value("${app.recognizer.dtw-max-series-points:96}") int dtwMaxSeriesPoints) {
        this.signSampleRepository = signSampleRepository;
        this.objectMapper = objectMapper;
        this.dtwMaxSeriesPoints = Math.max(8, dtwMaxSeriesPoints);
    }

    @PostConstruct
    void warmUpOnStartup() {
        snapshot();
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

    @Override
    public void registerSample(String sampleId) {
        SignSample sample = signSampleRepository.findWithSignById(sampleId).orElse(null);
        if (sample == null) {
            return;
        }
        synchronized (this) {
            if (cached == null) {
                return;
            }
            var sign = sample.getSign();
            List<CachedSample> updated = new ArrayList<>();
            for (CachedSample entry : cached.samples()) {
                if (entry.signId().equals(sign.getId())) {
                    updated.add(new CachedSample(sign.getId(), sign.getLabel(), sign.isBimanual(), entry.series()));
                } else {
                    updated.add(entry);
                }
            }
            updated.add(new CachedSample(
                    sign.getId(),
                    sign.getLabel(),
                    sign.isBimanual(),
                    prepareStoredSeries(sample)));
            cached = new LexiconSnapshot(List.copyOf(updated));
        }
    }

    private LexiconSnapshot load() {
        List<CachedSample> samples = new ArrayList<>();
        for (SignSample sample : signSampleRepository.findAllWithSign()) {
            var sign = sample.getSign();
            samples.add(new CachedSample(
                    sign.getId(),
                    sign.getLabel(),
                    sign.isBimanual(),
                    prepareStoredSeries(sample)));
        }
        return new LexiconSnapshot(samples);
    }

    private PreparedStoredSeries prepareStoredSeries(SignSample sample) {
        List<FrameDto> frames = objectMapper.convertValue(sample.getFrames(), FRAME_LIST);
        ClipPayloadDto clip = ClipSegmentTailTrim.prepareClip(
                new ClipPayloadDto(sample.getDurationMs(), frames));
        return ClipTemporalDistance.prepareStored(
                objectMapper.valueToTree(clip.frames()), clip.durationMs(), dtwMaxSeriesPoints);
    }
}
