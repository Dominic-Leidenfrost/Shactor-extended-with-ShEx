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
        
        // Add START declaration if there are shapes
        if (!nodeShapes.isEmpty()) {
            NS first = nodeShapes.iterator().next();
            String startShape = extractShapeName(first.getTargetClass().toString());
            shexOutput.append("START=@").append(startShape).append("\n\n");
        }
        
        // Process each NodeShape in the input set
        for (NS nodeShape : nodeShapes) {
            processNodeShape(shexOutput, nodeShape);
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
        
        // Add common RDF/XML Schema namespaces
        output.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        output.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        
        // Add blank line for readability
        output.append("\n");
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
        for (int i = 0; i < propertyShapes.size(); i++) {
            PS propertyShape = propertyShapes.get(i);
            processPropertyShape(output, propertyShape);
            
            // Add semicolon separator except for last property
            if (i < propertyShapes.size() - 1) {
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
        output.append("  ").append(propertyPath).append(" ");
        
        // Handle constraints - either OR-list or simple constraints
        if (propertyShape.getHasOrList()) {
            processOrListConstraints(output, propertyShape);
        } else {
            processSimpleConstraints(output, propertyShape);
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
        // Handle common namespace patterns
        if (path.contains("#")) {
            return SHEX_PREFIX + ":" + path.substring(path.lastIndexOf("#") + 1);
        } else if (path.contains("/")) {
            return SHEX_PREFIX + ":" + path.substring(path.lastIndexOf("/") + 1);
        }
        
        // Fallback for unusual path formats
        return SHEX_PREFIX + ":" + path.replaceAll("[^a-zA-Z0-9]", "_");
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
        
        boolean hasIRIKind = false;
        boolean hasLiteralKind = false;
        boolean hasXsd = false;
        boolean hasNonXsd = false;
        for (ShaclOrListItem it : cleanItems) {
            if ("IRI".equals(it.getNodeKind())) hasIRIKind = true;
            if ("Literal".equals(it.getNodeKind())) hasLiteralKind = true;
            String dc = it.getDataTypeOrClass();
            if (dc != null) {
                if (dc.startsWith("xsd:") || dc.contains("XMLSchema#")) {
                    hasXsd = true;
                } else {
                    hasNonXsd = true;
                }
            }
        }
        
        // Create OR-expression with pipe separators
        for (int i = 0; i < cleanItems.size(); i++) {
            ShaclOrListItem item = cleanItems.get(i);
            formatConstraintItem(output, item);
            
            // Add pipe separator except for last item
            if (i < cleanItems.size() - 1) {
                output.append(" | ");
            }
        }
        
        // Heuristic: add inline comment if mixed kinds or mixed XSD/non-XSD
        if ((hasIRIKind && hasLiteralKind) || (hasXsd && hasNonXsd)) {
            output.append(" # complex or");
        }
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
            
            // Format the constraint based on node kind
            formatConstraint(output, propertyShape.getDataTypeOrClass(), propertyShape.getNodeKind());
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
    private void formatConstraintItem(StringBuilder output, ShaclOrListItem item) {
        formatConstraint(output, item.getDataTypeOrClass(), item.getNodeKind());
    }

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
    private void formatConstraint(StringBuilder output, String dataTypeOrClass, String nodeKind) {
        if ("IRI".equals(nodeKind)) {
            // IRI constraint - check if it's a class or just IRI
            if (dataTypeOrClass.contains("XMLSchema") || dataTypeOrClass.startsWith("xsd:")) {
                // This shouldn't happen for IRI node kind, but handle gracefully
                output.append("IRI");
            } else {
                // Class constraint
                output.append(formatDataTypeOrClass(dataTypeOrClass));
            }
        } else {
            // Literal constraint or unspecified - use datatype
            output.append(formatDataTypeOrClass(dataTypeOrClass));
        }
    }

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
        if ("IRI".equals(nodeKind)) {
            output.append("IRI");
        } else if ("Literal".equals(nodeKind)) {
            output.append("LITERAL");
        } else {
            output.append(".");
        }
    }

    /**
     * Formats a datatype or class specification into ShEx syntax.
     * 
     * This method converts full datatype/class IRIs into abbreviated ShEx syntax,
     * handling common XSD datatypes and custom class definitions.
     * 
     * @param dataTypeOrClass The full datatype or class IRI
     * @return A formatted datatype/class specification for ShEx
     */
    private String formatDataTypeOrClass(String dataTypeOrClass) {
        // Handle XSD datatypes
        if (dataTypeOrClass.contains("XMLSchema#")) {
            String xsdType = dataTypeOrClass.substring(dataTypeOrClass.lastIndexOf("#") + 1);
            return "xsd:" + xsdType;
        }
        
        // Handle other common namespace patterns
        if (dataTypeOrClass.contains("#")) {
            return SHEX_PREFIX + ":" + dataTypeOrClass.substring(dataTypeOrClass.lastIndexOf("#") + 1);
        } else if (dataTypeOrClass.contains("/")) {
            return SHEX_PREFIX + ":" + dataTypeOrClass.substring(dataTypeOrClass.lastIndexOf("/") + 1);
        }
        
        // Return as-is if no clear pattern
        return dataTypeOrClass;
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