package com.librasConnect.system.signs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.FrameDto;
import com.librasConnect.system.dto.v1.RawHandDto;

public final class ClipTemporalDistance {

    private static final double ROLE_MISMATCH_STEP = 0.35;

    private ClipTemporalDistance() {
    }

    public static double dtwAverageCost(ClipPayloadDto query, JsonNode storedFrames, int storedDurationMs,
            int maxSeriesPoints) {
        if (storedFrames == null || !storedFrames.isArray() || storedFrames.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        List<FrameDto> qSeq = framesWithHandsSorted(query.frames());
        List<TimedFrameNode> sSeq = storedFramesWithHandsSorted(storedFrames, storedDurationMs);
        if (qSeq.isEmpty() || sSeq.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        List<FrameDto> qs = subsampleUniformTimeFrames(qSeq, maxSeriesPoints);
        List<TimedFrameNode> ss = subsampleUniformTimeStored(sSeq, maxSeriesPoints);
        int n = qs.size();
        int m = ss.size();
        double[][] cost = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                cost[i][j] = frameCost(qs.get(i), ss.get(j).frame());
            }
        }
        return dtwNormalized(cost);
    }

    private static List<FrameDto> framesWithHandsSorted(List<FrameDto> frames) {
        List<FrameDto> out = new ArrayList<>();
        for (FrameDto f : frames) {
            if (f.hands() != null && !f.hands().isEmpty()) {
                out.add(f);
            }
        }
        out.sort(Comparator.comparingInt(FrameDto::t));
        return out;
    }

    private static List<TimedFrameNode> storedFramesWithHandsSorted(JsonNode arr, int durationMs) {
        int len = arr.size();
        List<TimedFrameNode> out = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            JsonNode f = arr.get(i);
            JsonNode hands = f.get("hands");
            if (hands != null && hands.isArray() && hands.size() > 0) {
                int t = readT(f, i, len, durationMs);
                out.add(new TimedFrameNode(t, f));
            }
        }
        out.sort(Comparator.comparingInt(TimedFrameNode::t));
        return out;
    }

    private record TimedFrameNode(int t, JsonNode frame) {
    }

    private static int readT(JsonNode frame, int indexInArray, int totalFrames, int durationMs) {
        if (frame.hasNonNull("t") && frame.get("t").canConvertToInt()) {
            return frame.get("t").asInt();
        }
        if (totalFrames <= 1) {
            return 0;
        }
        return (int) ((long) indexInArray * durationMs / (totalFrames - 1));
    }

    private static List<FrameDto> subsampleUniformTimeFrames(List<FrameDto> sorted, int maxPoints) {
        if (sorted.size() <= maxPoints) {
            return sorted;
        }
        int tMin = sorted.get(0).t();
        int tMax = sorted.get(sorted.size() - 1).t();
        int span = Math.max(tMax - tMin, 1);
        List<FrameDto> out = new ArrayList<>(maxPoints);
        for (int k = 0; k < maxPoints; k++) {
            int targetT = tMin + (int) Math.round((double) k * span / Math.max(maxPoints - 1, 1));
            out.add(nearestFrameDto(sorted, targetT));
        }
        return out;
    }

    private static List<TimedFrameNode> subsampleUniformTimeStored(List<TimedFrameNode> sorted, int maxPoints) {
        if (sorted.size() <= maxPoints) {
            return sorted;
        }
        int tMin = sorted.get(0).t();
        int tMax = sorted.get(sorted.size() - 1).t();
        int span = Math.max(tMax - tMin, 1);
        List<TimedFrameNode> out = new ArrayList<>(maxPoints);
        for (int k = 0; k < maxPoints; k++) {
            int targetT = tMin + (int) Math.round((double) k * span / Math.max(maxPoints - 1, 1));
            out.add(nearestTimedStored(sorted, targetT));
        }
        return out;
    }

    private static FrameDto nearestFrameDto(List<FrameDto> sorted, int targetT) {
        FrameDto best = sorted.get(0);
        int bestD = Math.abs(best.t() - targetT);
        for (FrameDto f : sorted) {
            int d = Math.abs(f.t() - targetT);
            if (d < bestD) {
                bestD = d;
                best = f;
            }
        }
        return best;
    }

    private static TimedFrameNode nearestTimedStored(List<TimedFrameNode> sorted, int targetT) {
        TimedFrameNode best = sorted.get(0);
        int bestD = Math.abs(best.t() - targetT);
        for (TimedFrameNode x : sorted) {
            int d = Math.abs(x.t() - targetT);
            if (d < bestD) {
                bestD = d;
                best = x;
            }
        }
        return best;
    }

    static double frameCost(FrameDto queryFrame, JsonNode storedFrame) {
        Map<String, RawHandDto> qByRole = new HashMap<>();
        for (RawHandDto h : queryFrame.hands()) {
            if (h.role() != null) {
                qByRole.put(h.role(), h);
            }
        }
        Map<String, JsonNode> sByRole = new HashMap<>();
        JsonNode shands = storedFrame.get("hands");
        if (shands != null && shands.isArray()) {
            for (JsonNode h : shands) {
                if (h.hasNonNull("role")) {
                    sByRole.put(h.get("role").asText(), h);
                }
            }
        }
        if (qByRole.isEmpty() || sByRole.isEmpty()) {
            return ROLE_MISMATCH_STEP;
        }
        double sum = 0;
        int n = 0;
        for (Map.Entry<String, RawHandDto> e : qByRole.entrySet()) {
            JsonNode sh = sByRole.get(e.getKey());
            if (sh != null) {
                Double mse = mseLandmarks(e.getValue().landmarks(), sh.get("landmarks"));
                if (mse != null) {
                    sum += mse;
                    n++;
                }
            }
        }
        if (n == 0) {
            return ROLE_MISMATCH_STEP;
        }
        return sum / n;
    }

    private static Double mseLandmarks(List<List<Double>> qPts, JsonNode lm) {
        if (lm == null || !lm.isArray() || lm.size() != 21 || qPts.size() != 21) {
            return null;
        }
        double acc = 0;
        int c = 0;
        for (int i = 0; i < 21; i++) {
            List<Double> a = qPts.get(i);
            JsonNode b = lm.get(i);
            if (a == null || a.size() < 3 || b == null || !b.isArray() || b.size() < 3) {
                return null;
            }
            for (int k = 0; k < 3; k++) {
                double dv = a.get(k) - b.get(k).asDouble();
                acc += dv * dv;
                c++;
            }
        }
        return c > 0 ? acc / c : null;
    }

    private static double dtwNormalized(double[][] cost) {
        int n = cost.length;
        int m = cost[0].length;
        double[][] dp = new double[n][m];
        dp[0][0] = cost[0][0];
        for (int i = 1; i < n; i++) {
            dp[i][0] = dp[i - 1][0] + cost[i][0];
        }
        for (int j = 1; j < m; j++) {
            dp[0][j] = dp[0][j - 1] + cost[0][j];
        }
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < m; j++) {
                double prev = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                dp[i][j] = cost[i][j] + prev;
            }
        }
        double total = dp[n - 1][m - 1];
        int steps = dtwPathLength(n, m, dp);
        return steps > 0 ? total / steps : Double.POSITIVE_INFINITY;
    }

    private static int dtwPathLength(int n, int m, double[][] dp) {
        int i = n - 1;
        int j = m - 1;
        int len = 0;
        while (i >= 0 && j >= 0) {
            len++;
            if (i == 0 && j == 0) {
                break;
            }
            if (i == 0) {
                j--;
            } else if (j == 0) {
                i--;
            } else {
                double a = dp[i - 1][j - 1];
                double b = dp[i - 1][j];
                double c = dp[i][j - 1];
                if (a <= b && a <= c) {
                    i--;
                    j--;
                } else if (b <= c) {
                    i--;
                } else {
                    j--;
                }
            }
        }
        return len;
    }
}
