package com.librasConnect.system.services.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.RecognizeResponseDto;
import com.librasConnect.system.exception.ApiException;
import com.librasConnect.system.services.RecognitionLexiconCache;
import com.librasConnect.system.services.RecognitionLexiconCache.CachedSample;
import com.librasConnect.system.services.RecognitionLexiconCache.LexiconSnapshot;
import com.librasConnect.system.services.RecognitionOrchestrator;
import com.librasConnect.system.signs.BimanualStats;
import com.librasConnect.system.signs.ClipComparison;
import com.librasConnect.system.signs.ClipPayloadValidator;
import com.librasConnect.system.signs.ClipSegmentTailTrim;
import com.librasConnect.system.signs.ClipTemporalDistance;
import com.librasConnect.system.signs.PreparedQuerySeries;
import com.librasConnect.system.signs.PreparedStoredSeries;

@Service
public class RecognitionOrchestratorImpl implements RecognitionOrchestrator {

    private final RecognitionLexiconCache lexiconCache;
    private final ClipPayloadValidator clipValidator;
    private final boolean enabled;
    private final double maxMeanDistance;
    private final double minGapToSecondSign;
    private final int dtwMaxSeriesPoints;
    private final int coarseSeriesPoints;
    private final int coarseTopSigns;

    public RecognitionOrchestratorImpl(
            RecognitionLexiconCache lexiconCache,
            ClipPayloadValidator clipValidator,
            @Value("${app.recognizer.enabled:true}") boolean enabled,
            @Value("${app.recognizer.max-mean-distance:0.012}") double maxMeanDistance,
            @Value("${app.recognizer.min-gap-next-sign:0.004}") double minGapToSecondSign,
            @Value("${app.recognizer.dtw-max-series-points:96}") int dtwMaxSeriesPoints,
            @Value("${app.recognizer.coarse-series-points:48}") int coarseSeriesPoints,
            @Value("${app.recognizer.coarse-top-signs:8}") int coarseTopSigns) {
        this.lexiconCache = lexiconCache;
        this.clipValidator = clipValidator;
        this.enabled = enabled;
        this.maxMeanDistance = maxMeanDistance;
        this.minGapToSecondSign = minGapToSecondSign;
        this.dtwMaxSeriesPoints = Math.max(8, dtwMaxSeriesPoints);
        int coarse = Math.max(0, coarseSeriesPoints);
        this.coarseSeriesPoints = coarse >= this.dtwMaxSeriesPoints ? 0 : coarse;
        this.coarseTopSigns = Math.max(1, coarseTopSigns);
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
        LexiconSnapshot lexicon = lexiconCache.snapshot();
        if (lexicon.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Não há expressões cadastradas para reconhecer. Cadastre amostras em Treinar conversa.",
                    "CATALOG_EMPTY");
        }
        PreparedQuerySeries querySeries = ClipTemporalDistance.prepareQuery(matchClip, dtwMaxSeriesPoints);
        if (querySeries.frames().isEmpty()) {
            return RecognizeResponseDto.notRecognized();
        }
        boolean queryBimanual = BimanualStats.isBimanualClip(matchClip);
        Set<String> candidateSignIds = resolveCandidateSignIds(lexicon, querySeries, queryBimanual);
        Map<String, Double> bestDistBySign = new HashMap<>();
        Map<String, String> labelBySign = new HashMap<>();
        for (CachedSample entry : lexicon.samples()) {
            if (!candidateSignIds.isEmpty() && !candidateSignIds.contains(entry.signId())) {
                continue;
            }
            if (!queryBimanual && entry.bimanual()) {
                continue;
            }
            labelBySign.put(entry.signId(), entry.label());
            double d = ClipComparison.temporalDistance(querySeries, entry.series());
            bestDistBySign.merge(entry.signId(), d, Math::min);
        }
        return rankAndRespond(bestDistBySign, labelBySign);
    }

    private Set<String> resolveCandidateSignIds(LexiconSnapshot lexicon, PreparedQuerySeries querySeries,
            boolean queryBimanual) {
        if (coarseSeriesPoints <= 0) {
            return Set.of();
        }
        PreparedQuerySeries coarseQuery = ClipTemporalDistance.subsampleQuery(querySeries, coarseSeriesPoints);
        Map<String, Double> coarseBest = new HashMap<>();
        for (CachedSample entry : lexicon.samples()) {
            if (!queryBimanual && entry.bimanual()) {
                continue;
            }
            PreparedStoredSeries coarseStored = ClipTemporalDistance.subsampleStored(entry.series(), coarseSeriesPoints);
            double d = ClipComparison.temporalDistance(coarseQuery, coarseStored);
            coarseBest.merge(entry.signId(), d, Math::min);
        }
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(coarseBest.entrySet());
        ranked.sort(Comparator.comparingDouble(Map.Entry::getValue));
        int limit = Math.min(coarseTopSigns, ranked.size());
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < limit; i++) {
            ids.add(ranked.get(i).getKey());
        }
        return ids;
    }

    private RecognizeResponseDto rankAndRespond(Map<String, Double> bestDistBySign, Map<String, String> labelBySign) {
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
