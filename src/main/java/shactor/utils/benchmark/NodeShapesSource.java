package shactor.utils.benchmark;

import cs.qse.common.structure.NS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shactor.utils.nodeshapes.NodeShapesSnapshotIO;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Helper to obtain NodeShapes inputs for formatter benchmarks/tests.
 * Now file-based: loads the latest snapshot from evaluation/latest-node-shapes.json.
 */
public final class NodeShapesSource {
    private static final Logger LOG = LoggerFactory.getLogger(NodeShapesSource.class);

    private NodeShapesSource() {}

    // Backward-compatible names (used by existing test/doc): now delegate to file-based snapshot

    /**
     * Returns a supplier that reads the latest Set<NS> from the snapshot file created by the app.
     */
    public static Supplier<Set<NS>> fromAppCache() {
        return NodeShapesSource::loadOnce;
    }

    /**
     * @return true if the snapshot file exists and is non-empty.
     */
    public static boolean appCacheAvailable() {
        if (!NodeShapesSnapshotIO.defaultSnapshotExists()) return false;
        Set<NS> s = NodeShapesSnapshotIO.loadDefault();
        boolean ok = s != null && !s.isEmpty();
        if (!ok) {
            LOG.warn("[NodeShapesSource] Snapshot file exists but appears empty.");
        } else {
            LOG.info("[NodeShapesSource] Snapshot available. nodeShapes={}", s.size());
        }
        return ok;
    }

    private static Set<NS> loadOnce() {
        if (!NodeShapesSnapshotIO.defaultSnapshotExists()) {
            LOG.warn("[NodeShapesSource] No snapshot file found at {}", NodeShapesSnapshotIO.DEFAULT_SNAPSHOT.toAbsolutePath());
            return Collections.emptySet();
        }
        return NodeShapesSnapshotIO.loadDefault();
    }
}
