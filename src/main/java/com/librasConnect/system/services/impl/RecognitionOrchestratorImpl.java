package com.librasConnect.system.services.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.RecognizeResponseDto;
import com.librasConnect.system.exception.ApiException;
import com.librasConnect.system.models.SignSample;
import com.librasConnect.system.repositories.SignRepository;
import com.librasConnect.system.repositories.SignSampleRepository;
import com.librasConnect.system.services.RecognitionOrchestrator;
import com.librasConnect.system.signs.ClipComparison;
import com.librasConnect.system.signs.ClipPayloadValidator;
import com.librasConnect.system.signs.ClipSegmentTailTrim;

@Service
public class RecognitionOrchestratorImpl implements RecognitionOrchestrator {

    private final SignRepository signRepository;
    private final SignSampleRepository signSampleRepository;
    private final ClipPayloadValidator clipValidator;
    private final boolean enabled;
    private final double maxMeanDistance;
    private final double minGapToSecondSign;
    private final int dtwMaxSeriesPoints;

    public RecognitionOrchestratorImpl(
            SignRepository signRepository,
            SignSampleRepository signSampleRepository,
            ClipPayloadValidator clipValidator,
            @Value("${app.recognizer.enabled:true}") boolean enabled,
            @Value("${app.recognizer.max-mean-distance:0.012}") double maxMeanDistance,
            @Value("${app.recognizer.min-gap-next-sign:0.004}") double minGapToSecondSign,
            @Value("${app.recognizer.dtw-max-series-points:96}") int dtwMaxSeriesPoints) {
        this.signRepository = signRepository;
        this.signSampleRepository = signSampleRepository;
        this.clipValidator = clipValidator;
        this.enabled = enabled;
        this.maxMeanDistance = maxMeanDistance;
        this.minGapToSecondSign = minGapToSecondSign;
        this.dtwMaxSeriesPoints = Math.max(8, dtwMaxSeriesPoints);
    }

    @Override
    @Transactional(readOnly = true)
    public RecognizeResponseDto recognize(ClipPayloadDto clip) {
        clipValidator.validate(clip);
        ClipPayloadDto matchClip = ClipSegmentTailTrim.trimTrailingFramesWithoutHands(clip);
        if (!enabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de reconhecimento temporariamente indisponível",
                    "RECOGNIZER_UNAVAILABLE");
        }
        if (signRepository.count() == 0 || signSampleRepository.count() == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Não há expressões cadastradas para reconhecer. Cadastre amostras em Treinar conversa.",
                    "CATALOG_EMPTY");
        }
        Map<String, Double> bestDistBySign = new HashMap<>();
        Map<String, String> labelBySign = new HashMap<>();
        for (SignSample sample : signSampleRepository.findAll()) {
            String signId = sample.getSign().getId();
            labelBySign.put(signId, sample.getSign().getLabel());
            double d = ClipComparison.temporalDistance(matchClip, sample.getFrames(), sample.getDurationMs(),
                    dtwMaxSeriesPoints);
            bestDistBySign.merge(signId, d, Math::min);
        }
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(bestDistBySign.entrySet());
        ranked.sort(Comparator.comparingDouble(Map.Entry::getValue));
        if (ranked.isEmpty()) {
            return RecognizeResponseDto.notRecognized();
        }
        Map.Entry<String, Double> best = ranked.get(0);
        double bestD = best.getValue();
        if (!Double.isFinite(bestD) || bestD > maxMeanDistance) {
            return RecognizeResponseDto.notRecognized();
        }
        if (ranked.size() >= 2) {
            double secondD = ranked.get(1).getValue();
            if (Double.isFinite(secondD) && (secondD - bestD) < minGapToSecondSign) {
                return RecognizeResponseDto.notRecognized();
            }
        }
        return RecognizeResponseDto.ok(best.getKey(), labelBySign.get(best.getKey()));
    }
}
