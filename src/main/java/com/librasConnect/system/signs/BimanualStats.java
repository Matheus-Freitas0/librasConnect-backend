package com.librasConnect.system.signs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public final class BimanualStats {

    private BimanualStats() {
    }

    public static double medianRatioTwoHands(JsonNode framesArray) {
        if (framesArray == null || !framesArray.isArray() || framesArray.size() == 0) {
            return 0;
        }
        int n = framesArray.size();
        int two = 0;
        for (JsonNode f : framesArray) {
            JsonNode hands = f.get("hands");
            int hc = hands != null && hands.isArray() ? hands.size() : 0;
            if (hc == 2) {
                two++;
            }
        }
        return (double) two / (double) n;
    }

    public static boolean isBimanualFromSampleRatios(List<Double> ratios) {
        if (ratios.isEmpty()) {
            return false;
        }
        List<Double> sorted = new ArrayList<>(ratios);
        Collections.sort(sorted);
        int mid = sorted.size() / 2;
        double median = sorted.size() % 2 == 1
                ? sorted.get(mid)
                : (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        return median >= 0.5;
    }
}
