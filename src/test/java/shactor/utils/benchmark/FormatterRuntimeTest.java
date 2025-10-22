package shactor.utils.benchmark;

import cs.qse.common.structure.NS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import shactor.utils.formatters.ShExFormatter;
import shactor.utils.formatters.ShaclFormatter;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import shactor.utils.benchmark.NodeShapesSource;

/**
 * Demonstrates simple runtime measurements for ShaclFormatter and ShExFormatter.
 *
 * This test is disabled by default to avoid slowing down CI. Enable locally by
 * removing @Disabled or running it explicitly from your IDE. Replace the inputSupplier
 * with real NodeShapes from your pipeline for meaningful results.
 */
@Disabled("Benchmark test - enable and run locally as needed")
class FormatterRuntimeTest {

    @Test
    void compareShaclAndShExRuntime() {
        // Use the snapshot saved by the app at evaluation/latest-node-shapes.json
        boolean available = NodeShapesSource.appCacheAvailable();
        System.out.println("[FormatterRuntimeTest] Snapshot available: " + available);
        System.out.println("[FormatterRuntimeTest] Snapshot path: " + shactor.utils.nodeshapes.NodeShapesSnapshotIO.DEFAULT_SNAPSHOT.toAbsolutePath());

        Supplier<Set<NS>> inputSupplier = available
                ? NodeShapesSource.fromAppCache()
                : () -> Collections.emptySet();

        if (!available) {
            System.out.println("[FormatterRuntimeTest][WARN] Snapshot not found or empty. Start the app, open a view to generate shapes, then rerun this test.");
        } else {
            // One-time load to report size
            Set<NS> sample = inputSupplier.get();
            System.out.println("[FormatterRuntimeTest] Loaded NodeShapes from snapshot. size=" + (sample != null ? sample.size() : 0));
        }

        ShaclFormatter shacl = new ShaclFormatter();
        ShExFormatter shex = new ShExFormatter();

        FormatterRuntime.CompareResult result = FormatterRuntime.compare(
                shacl,
                shex,
                inputSupplier,
                100,   // iterations
                5,    // repetitions per iteration
                3     // warmup iterations
        );

        System.out.println("[FormatterRuntimeTest] Runtime comparison:\n" + FormatterRuntime.report(result));
    }
}
