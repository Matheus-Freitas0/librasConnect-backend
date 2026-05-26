package com.librasConnect.system.signs;

import java.util.ArrayList;
import java.util.List;

import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.FrameDto;

public final class ClipSegmentTailTrim {

    private ClipSegmentTailTrim() {
    }

    public static ClipPayloadDto prepareClip(ClipPayloadDto clip) {
        ClipPayloadDto trimmed = trimTrailingFramesWithoutHands(clip);
        return trimLowQualityHandFrames(trimmed);
    }

    public static ClipPayloadDto trimTrailingFramesWithoutHands(ClipPayloadDto clip) {
        List<FrameDto> frames = clip.frames();
        if (frames.size() <= ClipPayloadValidator.MIN_FRAMES) {
            return clip;
        }
        ArrayList<FrameDto> copy = new ArrayList<>(frames);
        while (copy.size() > ClipPayloadValidator.MIN_FRAMES) {
            FrameDto last = copy.get(copy.size() - 1);
            boolean empty = last.hands() == null || last.hands().isEmpty();
            if (!empty) {
                break;
            }
            copy.remove(copy.size() - 1);
        }
        while (copy.size() > ClipPayloadValidator.MIN_FRAMES) {
            FrameDto first = copy.get(0);
            boolean empty = first.hands() == null || first.hands().isEmpty();
            if (!empty) {
                break;
            }
            copy.remove(0);
        }
        if (copy.size() == frames.size()) {
            return clip;
        }
        return withDurationFromFrames(clip.durationMs(), copy);
    }

    private static ClipPayloadDto trimLowQualityHandFrames(ClipPayloadDto clip) {
        ArrayList<FrameDto> copy = new ArrayList<>(clip.frames());
        while (copy.size() > ClipPayloadValidator.MIN_FRAMES) {
            if (!HandLandmarkSignals.frameHasLowQualityHands(copy.get(copy.size() - 1))) {
                break;
            }
            copy.remove(copy.size() - 1);
        }
        while (copy.size() > ClipPayloadValidator.MIN_FRAMES) {
            if (!HandLandmarkSignals.frameHasLowQualityHands(copy.get(0))) {
                break;
            }
            copy.remove(0);
        }
        if (copy.size() == clip.frames().size()) {
            return clip;
        }
        return withDurationFromFrames(clip.durationMs(), copy);
    }

    private static ClipPayloadDto withDurationFromFrames(int fallbackDurationMs, List<FrameDto> frames) {
        int durationMs = frames.isEmpty() ? fallbackDurationMs : frames.get(frames.size() - 1).t();
        return new ClipPayloadDto(durationMs, List.copyOf(frames));
    }
}
