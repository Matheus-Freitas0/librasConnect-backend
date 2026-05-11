package com.librasConnect.system.signs;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.FrameDto;
import com.librasConnect.system.dto.v1.RawHandDto;
import com.librasConnect.system.exception.ApiException;

@Component
public class ClipPayloadValidator {

    public static final int MIN_FRAMES = 2;
    public static final int MIN_FRAMES_WITH_HANDS = 2;
    public static final int MAX_FRAMES = 720;
    public static final int MIN_DURATION_MS = 50;
    public static final int MAX_DURATION_MS = 120_000;

    public void validate(ClipPayloadDto clip) {
        if (clip == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payload obrigatório");
        }
        if (clip.frames() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "frames é obrigatório");
        }
        List<FrameDto> frames = clip.frames();
        if (frames.size() < MIN_FRAMES || frames.size() > MAX_FRAMES) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "frames deve ter entre " + MIN_FRAMES + " e " + MAX_FRAMES + " entradas (sequência temporal do gesto)");
        }
        if (clip.durationMs() < MIN_DURATION_MS || clip.durationMs() > MAX_DURATION_MS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "durationMs deve estar entre " + MIN_DURATION_MS + " e " + MAX_DURATION_MS + " (duração do segmento gravado)");
        }
        int lastFrameT = frames.get(frames.size() - 1).t();
        if (clip.durationMs() + 100 < lastFrameT) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "durationMs deve cobrir o intervalo temporal dos frames (último t)");
        }
        Integer prevT = null;
        boolean hasHandFrame = false;
        for (FrameDto frame : frames) {
            if (prevT != null && frame.t() <= prevT) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "t em cada frame deve ser estritamente crescente (ms desde o início da gravação)");
            }
            prevT = frame.t();
            List<RawHandDto> hands = frame.hands() == null ? List.of() : frame.hands();
            if (hands.size() > 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cada frame pode ter no máximo 2 hands");
            }
            if (hands.size() >= 1) {
                hasHandFrame = true;
            }
            for (RawHandDto hand : hands) {
                if (hand.role() == null
                        || (!"left".equals(hand.role()) && !"right".equals(hand.role()))) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "role deve ser \"left\" ou \"right\"");
                }
                if (hand.landmarks() == null || hand.landmarks().size() != 21) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Cada mão deve ter exatamente 21 landmarks");
                }
                for (List<Double> pt : hand.landmarks()) {
                    if (pt == null || pt.size() != 3) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Cada landmark deve ter 3 coordenadas");
                    }
                    for (Double v : pt) {
                        if (v == null || !Double.isFinite(v)) {
                            throw new ApiException(HttpStatus.BAD_REQUEST, "Coordenadas devem ser números finitos");
                        }
                    }
                }
            }
        }
        if (!hasHandFrame) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "É necessário pelo menos um frame com pelo menos uma mão detectada");
        }
        int framesWithHands = 0;
        for (FrameDto frame : frames) {
            List<RawHandDto> hh = frame.hands() == null ? List.of() : frame.hands();
            if (!hh.isEmpty()) {
                framesWithHands++;
            }
        }
        if (framesWithHands < MIN_FRAMES_WITH_HANDS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "É necessário pelo menos " + MIN_FRAMES_WITH_HANDS
                            + " frames com mão detectada (frames sem mão são ignorados na comparação)");
        }
    }
}
