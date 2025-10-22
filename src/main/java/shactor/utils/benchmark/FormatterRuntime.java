package shactor.utils.benchmark;

import cs.qse.common.structure.NS;
import shactor.utils.formatters.ShapeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Minimal, dependency-free utility to benchmark ShapeFormatter implementations.
 *
 * Usage example:
 *   RuntimeStats stats = FormatterRuntime.measureFormatter(new ShaclFormatter(), () -> input, 10, 5, 3);
 *   CompareResult res = FormatterRuntime.compare(new ShaclFormatter(), new ShExFormatter(), () -> input, 10, 5, 3);
 */
public final class FormatterRuntime {
    private FormatterRuntime() {}

    private static volatile String BLACKHOLE; // avoid dead-code elimination

    public static void warmup(ShapeFormatter formatter, Supplier<Set<NS>> inputSupplier, int warmupIters) {
        if (formatter == null || inputSupplier == null || warmupIters <= 0) return;
        for (int i = 0; i < warmupIters; i++) {
            BLACKHOLE = formatter.formatShapes(inputSupplier.get());
        }
    }

    public static RuntimeStats measureFormatter(ShapeFormatter formatter,
                                                Supplier<Set<NS>> inputSupplier,
                                                int iterations,
                                                int repsPerIteration,
                                                int warmupIters) {
        Objects.requireNonNull(formatter, "formatter");
        Objects.requireNonNull(inputSupplier, "inputSupplier");
        if (iterations <= 0) iterations = 5;
        if (repsPerIteration <= 0) repsPerIteration = 1;
        if (warmupIters > 0) warmup(formatter, inputSupplier, warmupIters);

        List<Double> perRunMs = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            for (int r = 0; r < repsPerIteration; r++) {
                BLACKHOLE = formatter.formatShapes(inputSupplier.get());
            }
            long elapsed = System.nanoTime() - start;
            double msPerRun = (elapsed / (double) repsPerIteration) / 1_000_000.0;
            perRunMs.add(msPerRun);
        }
        return new RuntimeStats(perRunMs);
    }

    public static CompareResult compare(ShapeFormatter a,
                                        ShapeFormatter b,
                                        Supplier<Set<NS>> inputSupplier,
                                        int iterations,
                                        int repsPerIteration,
                                        int warmupIters) {
        RuntimeStats sa = measureFormatter(a, inputSupplier, iterations, repsPerIteration, warmupIters);
        RuntimeStats sb = measureFormatter(b, inputSupplier, iterations, repsPerIteration, warmupIters);
        return new CompareResult(a.getFormatName(), sa, b.getFormatName(), sb);
    }

    public static String report(CompareResult r) {
        return String.format(Locale.ROOT,
                "%s -> %s\n%s -> %s\n",
                r.nameA, r.statsA.toString(), r.nameB, r.statsB.toString());
    }

    public static final class CompareResult {
        public final String nameA;
        public final RuntimeStats statsA;
        public final String nameB;
        public final RuntimeStats statsB;

        public CompareResult(String nameA, RuntimeStats statsA, String nameB, RuntimeStats statsB) {
            this.nameA = nameA;
            this.statsA = statsA;
            this.nameB = nameB;
            this.statsB = statsB;
        }
    }
}
