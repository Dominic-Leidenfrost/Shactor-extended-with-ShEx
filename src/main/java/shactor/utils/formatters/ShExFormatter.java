package shactor.utils.formatters;

import cs.qse.common.structure.NS;
import cs.qse.common.structure.PS;
import cs.qse.common.structure.ShaclOrListItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * ShEx (Shape Expression Language) formatter implementation.
 * 
 * This class implements the ShapeFormatter interface to generate ShEx shapes
 * from NodeShape and PropertyShape data structures. ShEx is a language for
 * describing and validating RDF graph structures, providing an alternative
 * to SHACL with a more compact and readable syntax.
 * 
 * The formatter handles:
 * - Basic NodeShape definitions with target classes
 * - PropertyShape constraints including datatypes and node kinds
 * - Complex OR-list constraints using ShEx OR-expressions (|)
 * - IRI and Literal node kind specifications
 * - Proper ShEx namespace declarations and prefixes
 * 
 * ShEx syntax differs from SHACL in several key ways:
 * - More compact syntax without RDF triples
 * - Uses | for OR-expressions instead of sh:or lists
 * - Direct property constraints without intermediate PropertyShape resources
 * - Simplified datatype and class specifications
 * 
 * This implementation provides a clean, maintainable alternative to SHACL
 * while maintaining full compatibility with the existing data structures.
 * 
 * @author ShEx Integration Implementation - Phase 3
 * @version 1.0
 * @since Phase 3.1
 */
@Component
public class ShExFormatter implements ShapeFormatter {

    // ===== Helper API (exposed for testing minimal golden strings) =====
    // Note: These helpers are intentionally public to enable focused unit tests
    // without depending on external NS/PS classes from the QSE library.
    public static String formatCardinality(int min, Integer max) {
        if (min == 0 && max != null && max == 1) return "?";
        if (min == 1 && (max == null || max < 0)) return "+";
        if (min == 0 && (max == null || max < 0)) return "*";
        if (max == null || max < 0) return "{" + min + ",}"; // open upper bound
        if (min == 1 && max == 1) return ""; // default
        return "{" + min + "," + max + "}";
    }

    public static String emitValueSet(List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String v : values) {
            if (!first) sb.append(' ');
            first = false;
            sb.append(formatValue(v));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String disjunction(List<String> items) {
        return "( " + String.join(" OR ", items) + " )";
    }

    public static String formatPath(String path) {
        // helper to expose path normalization for tests
        return extractPropertyPathStatic(path);
    }

    private static String extractPropertyPathStatic(String path) {
        if (path == null) return "";
        // Inverse path
        if (path.startsWith("^")) {
            String base = path.substring(1);
            return "^" + toPrefixed(base);
        }
        return toPrefixed(path);
    }

    private static String toPrefixed(String iri) {
        if (iri == null) return "";
        String s = iri;
        // Direct namespace mapping by prefix
        if (s.startsWith(RDF_NAMESPACE)) {
            return "rdf:" + s.substring(RDF_NAMESPACE.length());
        }
        if (s.startsWith(XSD_NAMESPACE)) {
            return "xsd:" + s.substring(XSD_NAMESPACE.length());
        }
        if (s.startsWith(UB_NAMESPACE)) {
            return UB_PREFIX + ":" + s.substring(UB_NAMESPACE.length());
        }
        if (s.startsWith(SHEX_NAMESPACE)) {
            return SHEX_PREFIX + ":" + s.substring(SHEX_NAMESPACE.length());
        }
        // Hash or slash fallback
        if (s.contains("#")) {
            return SHEX_PREFIX + ":" + s.substring(s.lastIndexOf('#') + 1);
        } else if (s.contains("/")) {
            return SHEX_PREFIX + ":" + s.substring(s.lastIndexOf('/') + 1);
        }
        return SHEX_PREFIX + ":" + s.replaceAll("[^a-zA-Z0-9]", "_");
    }

    public static String emitClassShape(String classCurie, String shapeLabel) {
        // Emits: Label { rdf:type [ <classCurie> ] + }
        return shapeLabel + " { rdf:type " + emitValueSet(List.of(classCurie)) + " + }\n\n";
    }

    // ===== Internal helpers =====

    @SuppressWarnings("unchecked")
    private static java.util.List<String> reflectStringList(Object target, String... methodNames) {
        for (String m : methodNames) {
            try {
                java.lang.reflect.Method method = target.getClass().getMethod(m);
                Object val = method.invoke(target);
                if (val == null) continue;
                if (val instanceof java.util.List) {
                    java.util.List<?> l = (java.util.List<?>) val;
                    java.util.List<String> out = new java.util.ArrayList<>();
                    for (Object o : l) if (o != null) out.add(o.toString());
                    return out;
                } else if (val instanceof java.util.Collection) {
                    java.util.Collection<?> c = (java.util.Collection<?>) val;
                    java.util.List<String> out = new java.util.ArrayList<>();
                    for (Object o : c) if (o != null) out.add(o.toString());
                    return out;
                } else if (val.getClass().isArray()) {
                    int len = java.lang.reflect.Array.getLength(val);
                    java.util.List<String> out = new java.util.ArrayList<>(len);
                    for (int i = 0; i < len; i++) {
                        Object o = java.lang.reflect.Array.get(val, i);
                        if (o != null) out.add(o.toString());
                    }
                    return out;
                }
            } catch (Throwable ignored) { }
        }
        return null;
    }


    private void appendCardinality(StringBuilder output, PS propertyShape) {
        // Phase 1 — Cardinalities (make everything 0..*): enforce '*' for every triple constraint
        output.append(" *");
    }



    private static void appendPsAnnotations(StringBuilder output, PS propertyShape) {
        // Annotations disabled to ensure ShExC parser compatibility (no '% qse:...')
        return;
    }

    private static String formatValue(String v) {
        if (v == null) return "";
        String trimmed = v.trim();
        // Already quoted literal
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed;
        }
        // Full IRI -> CURIE via known namespaces, fallback to ex:
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return curieFromIriOrPrefixed(trimmed);
        }
        // Prefixed name
        if (isPrefixed(trimmed)) {
            return trimmed;
        }
        // Default: quote as string literal
        String escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"");
        return '"' + escaped + '"';
    }

    /**
     * ShEx namespace prefix for generated shapes.
     */
    private static final String SHEX_PREFIX = "ex";
    
    /**
     * ShEx namespace URI for shape definitions.
     */
    private static final String SHEX_NAMESPACE = "http://example.org/shapes/";
    
    /**
     * QSE (Quality Shape Extractor) namespace prefix for generated shapes.
     */
    private static final String QSE_PREFIX = "qse";
    
    /**
     * QSE namespace URI for shape definitions.
     */
    private static final String QSE_NAMESPACE = "http://shaclshapes.org/";

    /**
     * University Benchmark prefix and namespace.
     */
    private static final String UB_PREFIX = "ub";
    private static final String UB_NAMESPACE = "http://swat.cse.lehigh.edu/onto/univ-bench.owl#";

    /**
     * Common namespaces used for CURIE mapping.
     */
    private static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

    /**
     * Formats a set of NodeShapes and their PropertyShapes into ShEx syntax.
     * 
     * This method converts the internal NodeShape (NS) and PropertyShape (PS) data
     * structures into a valid ShEx representation. The process involves:
     * 
     * 1. Creating ShEx namespace prefix declarations
     * 2. Processing each NodeShape to create ShEx shape definitions
     * 3. Adding target class specifications using EXTRA declarations
     * 4. Processing PropertyShapes with path and constraint information
     * 5. Handling complex OR-list constraints using ShEx OR-expressions (|)
     * 6. Formatting the final output as clean ShEx syntax
     * 
     * ShEx syntax example:
     * ```
     * PREFIX ex: <http://example.org/shapes/>
     * PREFIX qse: <http://shaclshapes.org/>
     * 
     * ex:PersonShape {
     *   ex:name xsd:string ;
     *   ex:age xsd:integer ;
     *   ex:email xsd:string | IRI
     * }
     * ```
     * 
     * @param nodeShapes A set of NodeShape objects containing the shapes to format.
     *                   Each NodeShape includes its IRI, target class, and associated
     *                   PropertyShapes with their constraints.
     * @return A string representation of the shapes in ShEx syntax.
     * @throws IllegalArgumentException if nodeShapes is null
     * @throws RuntimeException if ShEx generation fails due to internal errors
     */
    // Map of class CURIE -> helper shape label (CURIE)
    private java.util.LinkedHashMap<String, String> requiredClassShapes = new java.util.LinkedHashMap<>();

    @Override
    public String formatShapes(Set<NS> nodeShapes) {
        // Input validation - ensure nodeShapes is not null
        if (nodeShapes == null) {
            throw new IllegalArgumentException("NodeShapes cannot be null");
        }

        // Create StringBuilder for efficient string construction
        StringBuilder shexOutput = new StringBuilder();
        
        // Add namespace prefix declarations
        addNamespacePrefixes(shexOutput);
        
        
        // Process each NodeShape in the input set
        for (NS nodeShape : nodeShapes) {
            processNodeShape(shexOutput, nodeShape);
        }

        // Emit required helper class shapes deterministically
        if (!requiredClassShapes.isEmpty()) {
            java.util.List<java.util.Map.Entry<String, String>> entries = new java.util.ArrayList<>(requiredClassShapes.entrySet());
            entries.sort(java.util.Comparator.comparing(java.util.Map.Entry::getValue));
            for (java.util.Map.Entry<String, String> e : entries) {
                shexOutput.append(emitClassShape(e.getKey(), e.getValue()));
            }
        }
        
        return shexOutput.toString();
    }

    /**
     * Adds the required namespace prefix declarations to the ShEx output.
     * 
     * This method adds standard namespace prefixes used in ShEx generation
     * to ensure clean and readable output with proper URI abbreviations.
     * 
     * @param output The StringBuilder to append namespace declarations to
     */
    private void addNamespacePrefixes(StringBuilder output) {
        // Add ShEx namespace prefix for shape declarations
        output.append("PREFIX ").append(SHEX_PREFIX).append(": <").append(SHEX_NAMESPACE).append(">\n");

        // Add QSE namespace prefix for shape IRI declarations
        output.append("PREFIX ").append(QSE_PREFIX).append(": <").append(QSE_NAMESPACE).append(">\n");

        // Add University Benchmark prefix
        output.append("PREFIX ").append(UB_PREFIX).append(": <").append(UB_NAMESPACE).append(">\n");

        // Add common RDF/XML Schema namespaces
        output.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        output.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");

        // Add blank line for readability
        output.append("\n");
    }

    // ====== Constraint helpers ======
    private static boolean isPrefixed(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        // Guard against absolute IRIs like http:// or https:// which are not prefixed names
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return false;
        // Basic prefixed name pattern
        return trimmed.matches("[A-Za-z_][A-Za-z0-9_-]*:.*");
    }

    private static boolean isXsd(String value) {
        if (value == null) return false;
        String v = value.trim();
        return v.startsWith("xsd:") || v.contains("XMLSchema#");
    }

    private static String curieFromIriOrPrefixed(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (isPrefixed(v)) return v;
        if (v.startsWith("http://") || v.startsWith("https://")) return toPrefixed(v);
        return v;
    }

    private static String buildShapeLabelFromCurie(String curie) {
        if (curie == null) {
            return SHEX_PREFIX + ":Shape";
        }
        String v = curie.trim();
        // If given a full IRI, normalize to a prefixed form first
        if (v.startsWith("http://") || v.startsWith("https://")) {
            v = toPrefixed(v);
        }
        // Extract local from prefixed name if possible
        int idx = v.indexOf(':');
        String local = idx > 0 ? v.substring(idx + 1) : v;
        // If local still looks like an IRI (e.g., contains "://"), fall back to fragment or last path segment
        if (local.contains("://")) {
            if (local.contains("#")) {
                local = local.substring(local.lastIndexOf('#') + 1);
            } else if (local.contains("/")) {
                local = local.substring(local.lastIndexOf('/') + 1);
            } else {
                local = local.replaceAll("[^a-zA-Z0-9]", "_");
            }
        }
        if (!local.endsWith("Shape")) local = local + "Shape";
        return SHEX_PREFIX + ":" + local;
    }

    private String ensureClassShapeRef(String iriOrCurie) {
        String curie = curieFromIriOrPrefixed(iriOrCurie);
        if (curie == null) return "";
        String label = requiredClassShapes.computeIfAbsent(curie, c -> buildShapeLabelFromCurie(c));
        return "@" + label;
    }

    private static String nodeKindToken(String nodeKind) {
        if (nodeKind == null) return null;
        String nk = nodeKind.trim();
        if (nk.equalsIgnoreCase("IRI") || nk.equalsIgnoreCase("Iri")) return "IRI";
        if (nk.equalsIgnoreCase("BNode") || nk.equalsIgnoreCase("BlankNode") || nk.equalsIgnoreCase("Blank Node")) return "BNode";
        if (nk.equalsIgnoreCase("Literal")) return "Literal";
        if (nk.equalsIgnoreCase("NonLiteral") || nk.equalsIgnoreCase("Non-literal") || nk.equalsIgnoreCase("Non Literal")) return "NonLiteral";
        return nk; // fallback
    }

    private String constraintToString(String dataTypeOrClass, String nodeKind) {
        String nk = nodeKindToken(nodeKind);
        if (nk != null) {
            switch (nk) {
                case "IRI":
                    if (dataTypeOrClass != null && !isXsd(dataTypeOrClass)) {
                        // Phase 3 — Enforce object node kind: require IRI AND target shape
                        return "IRI AND " + ensureClassShapeRef(dataTypeOrClass);
                    }
                    return "IRI";
                case "Literal":
                    if (dataTypeOrClass != null && isXsd(dataTypeOrClass)) {
                        // normalize to xsd:local
                        String v = dataTypeOrClass.contains("#")
                                ? ("xsd:" + dataTypeOrClass.substring(dataTypeOrClass.lastIndexOf('#') + 1))
                                : dataTypeOrClass;
                        return v.startsWith("xsd:") ? v : curieFromIriOrPrefixed(v);
                    }
                    return "Literal";
                case "BNode":
                case "NonLiteral":
                    return nk;
                default:
                    // Unknown token, fall back to datatype/class only
            }
        }
        // No nodeKind guidance
        if (dataTypeOrClass == null || "Undefined".equals(dataTypeOrClass)) return ".";
        if (isXsd(dataTypeOrClass)) {
            return dataTypeOrClass.contains("#")
                    ? ("xsd:" + dataTypeOrClass.substring(dataTypeOrClass.lastIndexOf('#') + 1))
                    : dataTypeOrClass;
        }
        // Phase 3 — Even without explicit nodeKind, object class implies IRI-only
        return "IRI AND " + ensureClassShapeRef(dataTypeOrClass);
    }

    /**
     * Processes a single NodeShape and adds it to the ShEx output.
     * 
     * This method handles the conversion of a NodeShape (NS) object into
     * ShEx shape definition syntax. It creates:
     * - Shape name based on NodeShape IRI
     * - Target class specification (if needed)
     * - All associated PropertyShapes as property constraints
     * 
     * ShEx shape syntax:
     * ```
     * ex:ShapeName {
     *   property1 constraint1 ;
     *   property2 constraint2 ;
     *   property3 constraint3
     * }
     * ```
     * 
     * @param output The StringBuilder to append the shape definition to
     * @param nodeShape The NodeShape object to process
     */
    private void processNodeShape(StringBuilder output, NS nodeShape) {
        // Extract shape name from targetClass and append "Shape" suffix
        String shapeName = extractShapeName(nodeShape.getTargetClass().toString());
        
        // Start shape definition
        output.append(shapeName).append(" {\n");
        
        // Process all PropertyShapes associated with this NodeShape
        List<PS> propertyShapes = nodeShape.getPropertyShapes();
        // Deterministic property order by path
        propertyShapes.sort(java.util.Comparator.comparing(ps -> extractPropertyPath(ps.getPath())));
        // Filter out constraints that would produce invalid ShExC (e.g., rdf:type .)
        java.util.List<PS> filtered = new java.util.ArrayList<>();
        for (PS ps : propertyShapes) {
            if (!shouldSkipPropertyShape(ps)) filtered.add(ps);
        }
        
        // Phase 2 — Explicit class typing: ensure an rdf:type value set for the target class exists
        boolean hasRdfType = false;
        for (PS ps : filtered) {
            String p = extractPropertyPath(ps.getPath());
            if ("ex:type".equals(p)) p = "rdf:type";
            if ("rdf:type".equals(p)) { hasRdfType = true; break; }
        }
        if (!hasRdfType) {
            String classCurie = curieFromIriOrPrefixed(nodeShape.getTargetClass().toString());
            output.append("  rdf:type ").append(emitValueSet(java.util.List.of(classCurie)));
            output.append(" +");
            if (!filtered.isEmpty()) {
                output.append(" ;\n");
            } else {
                output.append("\n");
            }
        }
        
        for (int i = 0; i < filtered.size(); i++) {
            PS propertyShape = filtered.get(i);
            processPropertyShape(output, propertyShape);
            
            // Add semicolon separator except for last property
            if (i < filtered.size() - 1) {
                output.append(" ;");
            }
            output.append("\n");
        }
        
        // Close shape definition
        output.append("}\n\n");
    }

    /**
     * Extracts a clean shape name from a full IRI.
     * 
     * This method converts full IRIs into abbreviated shape names suitable
     * for ShEx syntax. It handles both QSE namespace IRIs and other formats.
     * 
     * @param iri The full IRI string
     * @return A clean shape name with appropriate prefix
     */
    private String extractShapeName(String iri) {
        String local;
        String prefix;
        // Handle QSE namespace IRIs
        if (iri.startsWith(QSE_NAMESPACE)) {
            prefix = QSE_PREFIX;
            local = iri.substring(QSE_NAMESPACE.length());
        } else if (iri.contains("#")) {
            prefix = SHEX_PREFIX;
            local = iri.substring(iri.lastIndexOf("#") + 1);
        } else if (iri.contains("/")) {
            prefix = SHEX_PREFIX;
            local = iri.substring(iri.lastIndexOf("/") + 1);
        } else {
            prefix = SHEX_PREFIX;
            local = iri.replaceAll("[^a-zA-Z0-9]", "_");
        }
        // Append "Shape" suffix for ShEx shape labels derived from targetClass
        if (!local.endsWith("Shape")) {
            local = local + "Shape";
        }
        return prefix + ":" + local;
    }

    /**
     * Processes a single PropertyShape and adds it to the ShEx output.
     * 
     * This method handles the conversion of a PropertyShape (PS) object into
     * ShEx property constraint syntax. It manages:
     * - Property path extraction and formatting
     * - Simple datatype/class constraints
     * - Complex OR-list constraints using ShEx OR-expressions (|)
     * - Node kind specifications (IRI vs Literal)
     * 
     * ShEx property constraint examples:
     * - Simple: `ex:name xsd:string`
     * - OR-expression: `ex:value xsd:string | xsd:integer`
     * - IRI constraint: `ex:knows IRI`
     * 
     * @param output The StringBuilder to append the property constraint to
     * @param propertyShape The PropertyShape object to process
     */
    private void processPropertyShape(StringBuilder output, PS propertyShape) {
        // Extract and format property path
        String propertyPath = extractPropertyPath(propertyShape.getPath());
        // Fix wrong ex:type usage -> rdf:type
        if ("ex:type".equals(propertyPath)) {
            propertyPath = "rdf:type";
        }
        output.append("  ").append(propertyPath).append(" ");

        // Try enumeration (sh:in) first via reflection
        java.util.List<String> inValues = reflectStringList(propertyShape,
                "getIn", "getInList", "getShIn", "getShInList", "getValuesInSet", "getEnumeration", "getAllowedValues");
        if (inValues != null && !inValues.isEmpty()) {
            output.append(emitValueSet(inValues));
            if ("rdf:type".equals(propertyPath)) {
                output.append(" +");
            } else {
                appendCardinality(output, propertyShape);
            }
            appendPsAnnotations(output, propertyShape);
            return;
        }

        // Handle constraints - either OR-list or simple constraints
        if (propertyShape.getHasOrList()) {
            // Special handling for rdf:type: use value set of classes
            if ("rdf:type".equals(propertyPath)) {
                java.util.List<ShaclOrListItem> cleanItems = filterValidOrListItems(propertyShape.getShaclOrListItems());
                java.util.List<String> classCuries = new java.util.ArrayList<>();
                for (ShaclOrListItem item : cleanItems) {
                    String c = item.getDataTypeOrClass();
                    if (c != null && !isXsd(c)) {
                        classCuries.add(curieFromIriOrPrefixed(c));
                    }
                }
                if (!classCuries.isEmpty()) {
                    output.append(emitValueSet(classCuries));
                } else {
                    // fallback to generic disjunction
                    processOrListConstraints(output, propertyShape);
                }
                output.append(" +");
                appendPsAnnotations(output, propertyShape);
                return;
            }
            processOrListConstraints(output, propertyShape);
            appendCardinality(output, propertyShape);
            appendPsAnnotations(output, propertyShape);
        } else {
            if ("rdf:type".equals(propertyPath)) {
                String c = propertyShape.getDataTypeOrClass();
                if (c != null && !"Undefined".equals(c) && !isXsd(c)) {
                    output.append(emitValueSet(java.util.List.of(curieFromIriOrPrefixed(c))));
                    output.append(" +");
                    appendPsAnnotations(output, propertyShape);
                    return;
                }
            }
            processSimpleConstraints(output, propertyShape);
            appendCardinality(output, propertyShape);
            appendPsAnnotations(output, propertyShape);
        }
    }

    /**
     * Extracts a clean property path from a full property IRI.
     * 
     * This method converts full property IRIs into abbreviated property paths
     * suitable for ShEx syntax, handling common namespace patterns.
     * 
     * @param path The full property IRI string
     * @return A clean property path with appropriate prefix
     */
    private String extractPropertyPath(String path) {
        return extractPropertyPathStatic(path);
    }

    // Decide if a property shape should be skipped to avoid invalid ShExC (e.g., rdf:type .)
    private boolean shouldSkipPropertyShape(PS ps) {
        String propertyPath = extractPropertyPath(ps.getPath());
        if ("ex:type".equals(propertyPath)) propertyPath = "rdf:type";
        // Only skip logic applies to rdf:type
        if (!"rdf:type".equals(propertyPath)) return false;

        // If enumeration (sh:in) exists, don't skip
        java.util.List<String> inValues = reflectStringList(ps,
                "getIn", "getInList", "getShIn", "getShInList", "getValuesInSet", "getEnumeration", "getAllowedValues");
        if (inValues != null && !inValues.isEmpty()) return false;

        if (ps.getHasOrList()) {
            java.util.List<ShaclOrListItem> cleanItems = filterValidOrListItems(ps.getShaclOrListItems());
            for (ShaclOrListItem item : cleanItems) {
                String c = item.getDataTypeOrClass();
                if (c != null && !isXsd(c)) {
                    return false; // we have at least one class
                }
            }
            return true; // no usable classes
        }
        String c = ps.getDataTypeOrClass();
        if (c != null && !"Undefined".equals(c) && !isXsd(c)) return false;
        return true;
    }

    /**
     * Processes OR-list constraints for PropertyShapes with multiple datatype/class options.
     * 
     * This method handles complex PropertyShapes that allow multiple datatypes or classes
     * using ShEx's OR-expression syntax (|). It creates a pipe-separated list of valid
     * constraint options, filtering out undefined or invalid entries.
     * 
     * ShEx OR-expression examples:
     * - `xsd:string | xsd:integer`
     * - `IRI | xsd:anyURI`
     * - `ex:Person | ex:Organization`
     * 
     * @param output The StringBuilder to append the OR-expression to
     * @param propertyShape The PropertyShape containing OR-list constraints
     */
    private void processOrListConstraints(StringBuilder output, PS propertyShape) {
        // Filter out undefined or null ShaclOrListItems to get clean constraint list
        List<ShaclOrListItem> cleanItems = filterValidOrListItems(propertyShape.getShaclOrListItems());

        if (cleanItems.isEmpty()) {
            // No valid constraints - use generic constraint
            output.append(".");
            return;
        }

        // Build disjunction items deterministically (sorted for stability)
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (ShaclOrListItem item : cleanItems) {
            parts.add(constraintToString(item.getDataTypeOrClass(), item.getNodeKind()));
        }
        java.util.Collections.sort(parts);
        output.append(disjunction(parts));
    }

    /**
     * Filters ShaclOrListItems to remove undefined or invalid entries.
     * 
     * This method cleans the OR-list by removing items that have null or "Undefined"
     * datatype/class values, ensuring only valid constraints are processed.
     * 
     * @param orListItems The original list of ShaclOrListItems
     * @return A filtered list containing only valid items
     */
    private List<ShaclOrListItem> filterValidOrListItems(List<ShaclOrListItem> orListItems) {
        return orListItems.stream()
                .filter(item -> item.getDataTypeOrClass() != null && 
                               !item.getDataTypeOrClass().equals("Undefined"))
                .toList();
    }

    /**
     * Processes simple constraints for PropertyShapes without OR-lists.
     * 
     * This method handles PropertyShapes that have direct datatype/class and
     * node kind constraints without complex OR-list structures. It applies
     * constraints directly in ShEx syntax.
     * 
     * @param output The StringBuilder to append the constraint to
     * @param propertyShape The PropertyShape containing simple constraints
     */
    private void processSimpleConstraints(StringBuilder output, PS propertyShape) {
        // Check if we have a valid datatype/class constraint
        if (propertyShape.getDataTypeOrClass() != null &&
                !propertyShape.getDataTypeOrClass().equals("Undefined")) {
            output.append(constraintToString(propertyShape.getDataTypeOrClass(), propertyShape.getNodeKind()));
        } else if (propertyShape.getNodeKind() != null) {
            // Only node kind specified
            formatNodeKindConstraint(output, propertyShape.getNodeKind());
        } else {
            // No specific constraint - use generic
            output.append(".");
        }
    }

    /**
     * Formats a single constraint item from an OR-list.
     * 
     * This method formats individual ShaclOrListItem objects into ShEx constraint syntax,
     * handling both datatype and class constraints with appropriate node kind specifications.
     * 
     * @param output The StringBuilder to append the constraint to
     * @param item The ShaclOrListItem to format
     */

    /**
     * Formats a constraint with datatype/class and node kind information.
     * 
     * This method creates ShEx constraint syntax based on the datatype/class
     * and node kind specifications, handling XSD datatypes, custom classes,
     * and IRI/Literal node kinds appropriately.
     * 
     * @param output The StringBuilder to append the constraint to
     * @param dataTypeOrClass The datatype or class constraint
     * @param nodeKind The node kind specification ("IRI" or "Literal")
     */

    /**
     * Formats a node kind constraint without datatype/class information.
     * 
     * This method handles cases where only node kind is specified,
     * creating appropriate ShEx constraint syntax.
     * 
     * @param output The StringBuilder to append the constraint to
     * @param nodeKind The node kind specification ("IRI" or "Literal")
     */
    private void formatNodeKindConstraint(StringBuilder output, String nodeKind) {
        String token = nodeKindToken(nodeKind);
        if (token == null) {
            output.append(".");
            return;
        }
        switch (token) {
            case "IRI":
            case "BNode":
            case "Literal":
            case "NonLiteral":
                output.append(token);
                break;
            default:
                output.append(".");
        }
    }


    /**
     * Returns the format name for this formatter.
     * 
     * @return "ShEx" as the format identifier
     */
    @Override
    public String getFormatName() {
        return "ShEx";
    }

    /**
     * Returns the file extension for ShEx files.
     * 
     * @return "shex" as the standard file extension for ShEx files
     */
    @Override
    public String getFileExtension() {
        return "shex";
    }

    /**
     * Validates that the provided NodeShapes can be formatted as ShEx.
     * 
     * This method performs basic validation to ensure the input NodeShapes
     * contain the necessary information for ShEx generation. It checks for
     * null values and validates that NodeShapes have required properties.
     * 
     * @param nodeShapes The set of NodeShapes to validate
     * @return true if the NodeShapes can be formatted as ShEx, false otherwise
     * @throws IllegalArgumentException if nodeShapes is null
     */
    @Override
    public boolean canFormat(Set<NS> nodeShapes) {
        if (nodeShapes == null) {
            throw new IllegalArgumentException("NodeShapes cannot be null");
        }
        
        // Empty sets are valid (will produce empty ShEx output)
        if (nodeShapes.isEmpty()) {
            return true;
        }
        
        // Validate that all NodeShapes have required properties
        for (NS nodeShape : nodeShapes) {
            if (nodeShape.getIri() == null || nodeShape.getTargetClass() == null) {
                return false;
            }
            
            // Validate PropertyShapes if present
            if (nodeShape.getPropertyShapes() != null) {
                for (PS propertyShape : nodeShape.getPropertyShapes()) {
                    if (propertyShape.getIri() == null || propertyShape.getPath() == null) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
}