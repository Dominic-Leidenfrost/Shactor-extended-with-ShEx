package shactor.utils.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Simple container for runtime statistics based on multiple samples.
 * Provides median, p90, p95 in milliseconds.
 */
public class RuntimeStats {
    private final List<Double> samplesMs; // sorted copy
    private final double medianMs;
    private final double p90Ms;
    private final double p95Ms;

    public RuntimeStats(List<Double> samplesMs) {
        if (samplesMs == null || samplesMs.isEmpty()) {
            this.samplesMs = List.of();
            this.medianMs = 0.0;
            this.p90Ms = 0.0;
            this.p95Ms = 0.0;
            return;
        }
        List<Double> copy = new ArrayList<>(samplesMs);
        Collections.sort(copy);
        this.samplesMs = Collections.unmodifiableList(copy);
        this.medianMs = percentile(copy, 50);
        this.p90Ms = percentile(copy, 90);
        this.p95Ms = percentile(copy, 95);
    }

    private static double percentile(List<Double> sorted, int p) {
        if (sorted.isEmpty()) return 0.0;
        double rank = (p / 100.0) * (sorted.size());
        int idx = Math.max(0, Math.min(sorted.size() - 1, (int) Math.ceil(rank) - 1));
        return sorted.get(idx);
    }

    public List<Double> getSamplesMs() {
        return samplesMs;
    }

    public double getMedianMs() {
        return medianMs;
    }

    public double getP90Ms() {
        return p90Ms;
    }

    public double getP95Ms() {
        return p95Ms;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "median=%.3f ms, p90=%.3f ms, p95=%.3f ms (n=%d)",
                medianMs, p90Ms, p95Ms, samplesMs.size());
    }
}
