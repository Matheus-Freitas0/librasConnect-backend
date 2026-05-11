package com.librasConnect.system.services.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.PostTrainingSampleRequestDto;
import com.librasConnect.system.dto.v1.SampleMetaDto;
import com.librasConnect.system.exception.ApiException;
import com.librasConnect.system.models.Sign;
import com.librasConnect.system.models.SignSample;
import com.librasConnect.system.repositories.SignRepository;
import com.librasConnect.system.repositories.SignSampleRepository;
import com.librasConnect.system.services.TrainingSampleService;
import com.librasConnect.system.signs.BimanualStats;
import com.librasConnect.system.signs.ClipPayloadValidator;
import com.librasConnect.system.signs.SignLabelSlug;

@Service
public class TrainingSampleServiceImpl implements TrainingSampleService {

    private final SignRepository signRepository;
    private final SignSampleRepository signSampleRepository;
    private final ClipPayloadValidator clipValidator;
    private final ObjectMapper objectMapper;

    public TrainingSampleServiceImpl(
            SignRepository signRepository,
            SignSampleRepository signSampleRepository,
            ClipPayloadValidator clipValidator,
            ObjectMapper objectMapper) {
        this.signRepository = signRepository;
        this.signSampleRepository = signSampleRepository;
        this.clipValidator = clipValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SampleMetaDto submitSample(PostTrainingSampleRequestDto request) {
        String labelDisplay = SignLabelSlug.normalizeLabelWhitespace(request.label());
        if (labelDisplay.isEmpty() || labelDisplay.length() > 60) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "label deve ter entre 1 e 60 caracteres");
        }
        String signId = SignLabelSlug.toSignId(labelDisplay);
        ClipPayloadDto clip = new ClipPayloadDto(request.durationMs(), request.frames());
        clipValidator.validate(clip);
        String description = request.description() == null ? null
                : SignLabelSlug.normalizeLabelWhitespace(request.description());
        if (description != null && description.isEmpty()) {
            description = null;
        }
        if (description != null && description.length() > 280) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "description deve ter no máximo 280 caracteres");
        }
        ensureSignExists(signId, labelDisplay, description);
        Sign sign = signRepository.findById(signId)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao resolver sinal"));
        JsonNode framesJson = objectMapper.valueToTree(clip.frames());
        Instant now = Instant.now();
        SignSample sample = SignSample.builder()
                .id(newSampleId())
                .sign(sign)
                .createdAt(now)
                .durationMs(clip.durationMs())
                .frameCount(clip.frames().size())
                .frames(framesJson)
                .build();
        signSampleRepository.save(sample);
        refreshSignAggregates(signId);
        return new SampleMetaDto(sample.getId(), signId, sample.getCreatedAt(), sample.getDurationMs(),
                sample.getFrameCount());
    }

    private void ensureSignExists(String signId, String labelDisplay, String description) {
        if (signRepository.existsById(signId)) {
            return;
        }
        Instant now = Instant.now();
        Sign sign = Sign.builder()
                .id(signId)
                .label(labelDisplay)
                .description(description != null && !description.isEmpty() ? description : null)
                .bimanual(false)
                .staticForm(true)
                .sampleCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            signRepository.save(sign);
        } catch (DataIntegrityViolationException ex) {
            if (!signRepository.existsById(signId)) {
                throw ex;
            }
        }
    }

    private void refreshSignAggregates(String signId) {
        Sign sign = signRepository.findById(signId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sinal não encontrado"));
        List<SignSample> samples = signSampleRepository.findBySign_IdOrderByCreatedAtAsc(signId);
        List<Double> ratios = new ArrayList<>();
        for (SignSample s : samples) {
            ratios.add(BimanualStats.medianRatioTwoHands(s.getFrames()));
        }
        sign.setSampleCount(samples.size());
        sign.setBimanual(BimanualStats.isBimanualFromSampleRatios(ratios));
        sign.setUpdatedAt(Instant.now());
        signRepository.save(sign);
    }

    private static String newSampleId() {
        return "smp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 26);
    }
}
