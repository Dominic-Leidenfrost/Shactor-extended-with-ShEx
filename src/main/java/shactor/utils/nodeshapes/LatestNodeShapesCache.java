package shactor.utils.nodeshapes;

import cs.qse.common.structure.NS;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory cache for the latest Set<NS> produced by the app.
 *
 * Minimal and non-invasive: callers can store the most recent NodeShapes set
 * so that benchmarks/tests can access the identical input later in the same JVM session.
 */
public final class LatestNodeShapesCache {
    private static final AtomicReference<Set<NS>> LATEST = new AtomicReference<>();

    private LatestNodeShapesCache() {}

    /**
     * Save a defensive copy of the provided set as the latest snapshot.
     */
    public static void save(Set<NS> nodeShapes) {
        if (nodeShapes == null) return;
        // Defensive copy to decouple from external mutation
        final Set<NS> copy = new LinkedHashSet<>(nodeShapes);
        // Keep the larger snapshot to avoid overwriting a full set with a tiny preview set
        LATEST.updateAndGet(cur -> {
            if (cur == null) return copy;
            return copy.size() >= cur.size() ? copy : cur;
        });
    }

    /**
     * Returns an unmodifiable snapshot of the latest saved set.
     * If nothing was saved yet, returns an empty set.
     */
    public static Set<NS> get() {
        Set<NS> cur = LATEST.get();
        if (cur == null) return Collections.emptySet();
        return Collections.unmodifiableSet(cur);
    }

    /**
     * @return true if no set was saved yet or the saved set is empty.
     */
    public static boolean isEmpty() {
        Set<NS> cur = LATEST.get();
        return cur == null || cur.isEmpty();
    }
}
