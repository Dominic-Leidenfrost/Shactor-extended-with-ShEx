package shactor.utils.nodeshapes;

import cs.qse.common.structure.NS;
import cs.qse.common.structure.PS;
import cs.qse.common.structure.ShaclOrListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * File-based persistence for the latest Set<NS> snapshot.
 *
 * This stores a JSON snapshot at a stable path so benchmarks/tests can load
 * exactly the same NodeShapes input later, even across JVM runs.
 */
public final class NodeShapesSnapshotIO {
    private static final Logger LOG = LoggerFactory.getLogger(NodeShapesSnapshotIO.class);

    public static final Path DEFAULT_SNAPSHOT = Path.of("evaluation", "latest-node-shapes.json");

    private NodeShapesSnapshotIO() {}

    // ===================== Public API =====================

    public static void saveDefault(Set<NS> nodeShapes) {
        save(nodeShapes, DEFAULT_SNAPSHOT);
    }

    public static void save(Set<NS> nodeShapes, Path file) {
        Objects.requireNonNull(file, "file");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            LOG.warn("[NodeShapesSnapshotIO] Failed to create directories for {}: {}", file, e.toString());
        }

        Snapshot snapshot = toSnapshot(nodeShapes);
        try {
            String json = toJson(snapshot);
            Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOG.info("[NodeShapesSnapshotIO] Saved NodeShapes snapshot: path={}, nodeShapes={}, propertyShapesTotal={}",
                    file.toAbsolutePath(),
                    snapshot.nodes != null ? snapshot.nodes.size() : 0,
                    snapshot.totalPsCount());
        } catch (Exception e) {
            LOG.error("[NodeShapesSnapshotIO] Error saving snapshot to {}: {}", file.toAbsolutePath(), e.toString(), e);
        }
    }

    public static Set<NS> loadDefault() {
        return load(DEFAULT_SNAPSHOT);
    }

    public static Set<NS> load(Path file) {
        Objects.requireNonNull(file, "file");
        if (!Files.exists(file)) {
            LOG.warn("[NodeShapesSnapshotIO] Snapshot file not found: {}", file.toAbsolutePath());
            return Collections.emptySet();
        }
        try {
            String json = Files.readString(file);
            Snapshot snapshot = fromJson(json);
            Set<NS> set = fromSnapshot(snapshot);
            LOG.info("[NodeShapesSnapshotIO] Loaded NodeShapes snapshot: path={}, nodeShapes={}, propertyShapesTotal={}",
                    file.toAbsolutePath(),
                    snapshot.nodes != null ? snapshot.nodes.size() : 0,
                    snapshot.totalPsCount());
            return set;
        } catch (Exception e) {
            LOG.error("[NodeShapesSnapshotIO] Error loading snapshot from {}: {}", file.toAbsolutePath(), e.toString(), e);
            return Collections.emptySet();
        }
    }

    public static boolean snapshotExists(Path file) {
        return Files.exists(file) && Files.isRegularFile(file);
    }

    public static boolean defaultSnapshotExists() {
        return snapshotExists(DEFAULT_SNAPSHOT);
    }

    // ===================== Mapping =====================

    private static Snapshot toSnapshot(Set<NS> nodeShapes) {
        Snapshot s = new Snapshot();
        if (nodeShapes == null || nodeShapes.isEmpty()) {
            s.nodes = List.of();
            return s;
        }
        List<SnapshotNS> nodes = new ArrayList<>();
        for (NS ns : nodeShapes) {
            if (ns == null) continue;
            SnapshotNS x = new SnapshotNS();
            try {
                x.iri = ns.getIri() != null ? ns.getIri().toString() : null;
                x.targetClass = ns.getTargetClass() != null ? ns.getTargetClass().toString() : null;
                try { x.support = ns.getSupport(); } catch (Throwable ignored) {}
                List<PS> psList = ns.getPropertyShapes();
                if (psList != null) {
                    List<SnapshotPS> sps = new ArrayList<>();
                    for (PS ps : psList) sps.add(toSnapshotPS(ps));
                    x.propertyShapes = sps;
                }
            } catch (Throwable e) {
                LOG.warn("[NodeShapesSnapshotIO] Skipping NS due to error: {}", e.toString());
            }
            nodes.add(x);
        }
        s.nodes = nodes;
        return s;
    }

    private static SnapshotPS toSnapshotPS(PS ps) {
        SnapshotPS x = new SnapshotPS();
        if (ps == null) return x;
        try {
            x.iri = ps.getIri() != null ? ps.getIri().toString() : null;
            x.path = ps.getPath();
            x.dataTypeOrClass = ps.getDataTypeOrClass();
            x.nodeKind = ps.getNodeKind();
            try { x.support = ps.getSupport(); } catch (Throwable ignored) {}
            try { x.confidence = ps.getConfidence(); } catch (Throwable ignored) {}
            try { x.hasOrList = ps.getHasOrList(); } catch (Throwable ignored) {}
            try {
                List<ShaclOrListItem> items = ps.getShaclOrListItems();
                if (items != null) {
                    x.orItems = items.stream().map(NodeShapesSnapshotIO::toSnapshotOrItem).collect(Collectors.toList());
                }
            } catch (Throwable ignored) {}
        } catch (Throwable e) {
            LOG.warn("[NodeShapesSnapshotIO] Error mapping PS: {}", e.toString());
        }
        return x;
    }

    private static SnapshotOrItem toSnapshotOrItem(ShaclOrListItem item) {
        SnapshotOrItem x = new SnapshotOrItem();
        if (item == null) return x;
        try {
            x.dataTypeOrClass = item.getDataTypeOrClass();
            x.nodeKind = item.getNodeKind();
            try { x.support = item.getSupport(); } catch (Throwable ignored) {}
            try { x.confidence = item.getConfidence(); } catch (Throwable ignored) {}
        } catch (Throwable e) {
            LOG.warn("[NodeShapesSnapshotIO] Error mapping OR item: {}", e.toString());
        }
        return x;
    }

    private static Set<NS> fromSnapshot(Snapshot snapshot) {
        if (snapshot == null || snapshot.nodes == null || snapshot.nodes.isEmpty()) return Collections.emptySet();
        java.util.LinkedHashSet<NS> set = new java.util.LinkedHashSet<>();
        for (SnapshotNS x : snapshot.nodes) {
            try {
                NS ns = new NS();
                org.eclipse.rdf4j.model.ValueFactory vf = SimpleValueFactory.getInstance();
                if (x.iri != null) ns.setIri(vf.createIRI(x.iri));
                if (x.targetClass != null) ns.setTargetClass(vf.createIRI(x.targetClass));
                if (x.support != null) {
                    try {
                        java.lang.reflect.Method m = ns.getClass().getMethod("setSupport", Integer.class);
                        m.invoke(ns, x.support);
                    } catch (Throwable ignored) {}
                }
                if (x.propertyShapes != null) {
                    List<PS> psList = new ArrayList<>();
                    for (SnapshotPS p : x.propertyShapes) psList.add(fromSnapshotPS(p));
                    ns.setPropertyShapes(psList);
                }
                set.add(ns);
            } catch (Throwable e) {
                LOG.warn("[NodeShapesSnapshotIO] Skipping NS during load due to error: {}", e.toString());
            }
        }
        return set;
    }

    private static PS fromSnapshotPS(SnapshotPS x) {
        PS ps = new PS();
        try {
            org.eclipse.rdf4j.model.ValueFactory vf = SimpleValueFactory.getInstance();
            if (x.iri != null) ps.setIri(vf.createIRI(x.iri));
            if (x.path != null) ps.setPath(x.path);
            if (x.dataTypeOrClass != null) ps.setDataTypeOrClass(x.dataTypeOrClass);
            if (x.nodeKind != null) ps.setNodeKind(x.nodeKind);
            if (x.support != null) { try { ps.setSupport(x.support); } catch (Throwable ignored) {} }
            if (x.confidence != null) { try { ps.setConfidence(x.confidence); } catch (Throwable ignored) {} }
            if (x.hasOrList != null) { try { ps.setHasOrList(x.hasOrList); } catch (Throwable ignored) {} }
            if (x.orItems != null && !x.orItems.isEmpty()) {
                List<ShaclOrListItem> items = new ArrayList<>();
                for (SnapshotOrItem s : x.orItems) {
                    ShaclOrListItem it = new ShaclOrListItem();
                    try {
                        if (s.dataTypeOrClass != null) it.setDataTypeOrClass(s.dataTypeOrClass);
                        if (s.nodeKind != null) it.setNodeKind(s.nodeKind);
                        if (s.support != null) { try { it.setSupport(s.support); } catch (Throwable ignored) {} }
                        if (s.confidence != null) { try { it.setConfidence(s.confidence); } catch (Throwable ignored) {} }
                    } catch (Throwable ignored) {}
                    items.add(it);
                }
                try { ps.setShaclOrListItems(items); } catch (Throwable ignored) {}
            }
        } catch (Throwable e) {
            LOG.warn("[NodeShapesSnapshotIO] Error reconstructing PS: {}", e.toString());
        }
        return ps;
    }

    // ===================== JSON helpers (Jackson) =====================

    private static String toJson(Snapshot s) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.findAndRegisterModules();
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(s);
    }

    private static Snapshot fromJson(String json) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.findAndRegisterModules();
        return om.readValue(json, Snapshot.class);
    }

    // ===================== DTOs =====================

    public static class Snapshot {
        public List<SnapshotNS> nodes;
        public int totalPsCount() {
            if (nodes == null) return 0;
            int sum = 0;
            for (SnapshotNS n : nodes) if (n != null && n.propertyShapes != null) sum += n.propertyShapes.size();
            return sum;
        }
    }

    public static class SnapshotNS {
        public String iri;
        public String targetClass;
        public Integer support;
        public List<SnapshotPS> propertyShapes;
    }

    public static class SnapshotPS {
        public String iri;
        public String path;
        public String dataTypeOrClass;
        public String nodeKind;
        public Integer support;
        public Double confidence;
        public Boolean hasOrList;
        public List<SnapshotOrItem> orItems;
    }

    public static class SnapshotOrItem {
        public String dataTypeOrClass;
        public String nodeKind;
        public Integer support;
        public Double confidence;
    }
}
