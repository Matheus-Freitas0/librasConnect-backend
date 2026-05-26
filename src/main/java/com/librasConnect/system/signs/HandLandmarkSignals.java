package com.librasConnect.system.signs;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.librasConnect.system.dto.v1.FrameDto;
import com.librasConnect.system.dto.v1.RawHandDto;

final class HandLandmarkSignals {

    private static final double LOW_QUALITY_WRIST_Y_MAX = 0.985;
    private static final double LOW_QUALITY_WRIST_Y_MIN = 0.02;

    private HandLandmarkSignals() {
    }

    static boolean frameHasLowQualityHands(FrameDto frame) {
        if (frame.hands() == null) {
            return false;
        }
        for (RawHandDto hand : frame.hands()) {
            if (handIsLowQuality(hand)) {
                return true;
            }
        }
        return false;
    }

    static boolean storedFrameHasLowQualityHands(JsonNode frame) {
        JsonNode hands = frame.get("hands");
        if (hands == null || !hands.isArray()) {
            return false;
        }
        for (JsonNode hand : hands) {
            if (storedHandIsLowQuality(hand)) {
                return true;
            }
        }
        return false;
    }

    private static List<Double> wrist(RawHandDto hand) {
        if (hand.landmarks() == null || hand.landmarks().isEmpty()) {
            return null;
        }
        return hand.landmarks().get(0);
    }

    private static boolean handIsLowQuality(RawHandDto hand) {
        List<Double> wrist = wrist(hand);
        if (wrist == null || wrist.size() < 2) {
            return true;
        }
        double y = wrist.get(1);
        return y > LOW_QUALITY_WRIST_Y_MAX || y < LOW_QUALITY_WRIST_Y_MIN;
    }

    private static boolean storedHandIsLowQuality(JsonNode hand) {
        JsonNode lm = hand.get("landmarks");
        if (lm == null || !lm.isArray() || lm.isEmpty()) {
            return true;
        }
        JsonNode wrist = lm.get(0);
        if (wrist == null || !wrist.isArray() || wrist.size() < 2) {
            return true;
        }
        double y = wrist.get(1).asDouble();
        return y > LOW_QUALITY_WRIST_Y_MAX || y < LOW_QUALITY_WRIST_Y_MIN;
    }
}
