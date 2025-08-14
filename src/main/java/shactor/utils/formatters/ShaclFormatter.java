package shactor.utils.formatters;

import cs.qse.common.structure.NS;
import cs.qse.common.structure.PS;
import cs.qse.common.structure.ShaclOrListItem;
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SHACL (Shapes Constraint Language) formatter implementation.
 * 
 * This class implements the ShapeFormatter interface to generate SHACL shapes
 * in Turtle syntax from NodeShape and PropertyShape data structures. It extracts
 * and refactors the existing SHACL generation logic from the Utils class into
 * a clean, maintainable, and testable implementation.
 * 
 * The formatter handles:
 * - Basic NodeShape definitions with target classes
 * - PropertyShape constraints including datatypes and node kinds
 * - Complex OR-list constraints (ShaclOrListItem structures)
 * - IRI and Literal node kind specifications
 * - Proper SHACL namespace declarations and prefixes
 * 
 * This implementation maintains full compatibility with the existing SHACL
 * generation logic while providing a cleaner architecture for future extensions.
 * 
 * @author ShEx Integration Implementation - Phase 2
 * @version 1.0
 * @since Phase 2.1
 */
@Component
public class ShaclFormatter implements ShapeFormatter {

    /**
     * SHACL namespace prefix used in generated Turtle output.
     */
    private static final String SHACL_PREFIX = "sh";
    
    /**
     * SHACL namespace URI.
     */
    private static final String SHACL_NAMESPACE = "http://www.w3.org/ns/shacl#";
    
    /**
     * QSE (Quality Shape Extractor) namespace prefix for generated shapes.
     */
    private static final String QSE_PREFIX = "qse";
    
    /**
     * QSE namespace URI for shape definitions.
     */
    private static final String QSE_NAMESPACE = "http://shaclshapes.org/";

    /**
     * Formats a set of NodeShapes and their PropertyShapes into SHACL Turtle syntax.
     * 
     * This method converts the internal NodeShape (NS) and PropertyShape (PS) data
     * structures into a valid SHACL representation using Apache Jena RDF model.
     * The process involves:
     * 
     * 1. Creating an RDF model with proper SHACL namespaces
     * 2. Processing each NodeShape to create sh:NodeShape declarations
     * 3. Adding target class specifications (sh:targetClass)
     * 4. Processing PropertyShapes with path and constraint information
     * 5. Handling complex OR-list constraints for multiple datatypes/classes
     * 6. Formatting the final model as Turtle syntax
     * 
     * @param nodeShapes A set of NodeShape objects containing the shapes to format.
     *                   Each NodeShape includes its IRI, target class, and associated
     *                   PropertyShapes with their constraints.
     * @return A string representation of the shapes in SHACL Turtle format.
     * @throws IllegalArgumentException if nodeShapes is null
     * @throws RuntimeException if SHACL generation fails due to internal errors
     */
    @Override
    public String formatShapes(Set<NS> nodeShapes) {
        // Input validation - ensure nodeShapes is not null
        if (nodeShapes == null) {
            throw new IllegalArgumentException("NodeShapes cannot be null");
        }

        // Create Apache Jena RDF model for SHACL generation
        Model model = ModelFactory.createDefaultModel();
        
        // Set up namespace prefixes for clean Turtle output
        setupNamespacePrefixes(model);
        
        // Process each NodeShape in the input set
        for (NS nodeShape : nodeShapes) {
            processNodeShape(model, nodeShape);
        }
        
        // Convert the RDF model to Turtle format and return as string
        return formatModelAsTurtle(model);
    }

    /**
     * Sets up the required namespace prefixes for SHACL generation.
     * 
     * This method configures the RDF model with standard SHACL and QSE
     * namespace prefixes to ensure clean and readable Turtle output.
     * 
     * @param model The Apache Jena RDF model to configure
     */
    private void setupNamespacePrefixes(Model model) {
        // Add SHACL namespace prefix for sh: declarations
        model.setNsPrefix(SHACL_PREFIX, SHACL_NAMESPACE);
        
        // Add QSE namespace prefix for shape IRI declarations
        model.setNsPrefix(QSE_PREFIX, QSE_NAMESPACE);
    }

    /**
     * Processes a single NodeShape and adds it to the RDF model.
     * 
     * This method handles the conversion of a NodeShape (NS) object into
     * SHACL RDF statements. It creates:
     * - rdf:type sh:NodeShape declaration
     * - sh:targetClass specification
     * - All associated PropertyShapes
     * 
     * @param model The RDF model to add statements to
     * @param nodeShape The NodeShape object to process
     */
    private void processNodeShape(Model model, NS nodeShape) {
        // Create resource for the NodeShape IRI
        Resource nodeShapeResource = ResourceFactory.createResource(nodeShape.getIri().toString());
        
        // Add rdf:type sh:NodeShape statement
        Statement nodeShapeTypeStatement = ResourceFactory.createStatement(
            nodeShapeResource,
            ResourceFactory.createProperty(RDF.type.toString()),
            ResourceFactory.createResource(SHACL.NODE_SHAPE.toString())
        );
        model.add(nodeShapeTypeStatement);
        
        // Add sh:targetClass statement to specify which class this shape validates
        Statement targetClassStatement = ResourceFactory.createStatement(
            nodeShapeResource,
            ResourceFactory.createProperty(SHACL.TARGET_CLASS.toString()),
            ResourceFactory.createResource(nodeShape.getTargetClass().toString())
        );
        model.add(targetClassStatement);
        
        // Process all PropertyShapes associated with this NodeShape
        for (PS propertyShape : nodeShape.getPropertyShapes()) {
            processPropertyShape(model, nodeShape, propertyShape);
        }
    }

    /**
     * Processes a single PropertyShape and adds it to the RDF model.
     * 
     * This method handles the conversion of a PropertyShape (PS) object into
     * SHACL RDF statements. It manages:
     * - Basic PropertyShape declarations (rdf:type, sh:path)
     * - Simple datatype/class constraints
     * - Complex OR-list constraints for multiple options
     * - Node kind specifications (IRI vs Literal)
     * 
     * @param model The RDF model to add statements to
     * @param nodeShape The parent NodeShape containing this PropertyShape
     * @param propertyShape The PropertyShape object to process
     */
    private void processPropertyShape(Model model, NS nodeShape, PS propertyShape) {
        // Create resources for NodeShape and PropertyShape IRIs
        Resource nodeShapeResource = ResourceFactory.createResource(nodeShape.getIri().toString());
        Resource propertyShapeResource = ResourceFactory.createResource(propertyShape.getIri().toString());
        
        // Link NodeShape to PropertyShape via sh:property
        Statement nodeShapePropertyStatement = ResourceFactory.createStatement(
            nodeShapeResource,
            ResourceFactory.createProperty(SHACL.PROPERTY.toString()),
            propertyShapeResource
        );
        model.add(nodeShapePropertyStatement);
        
        // Add sh:path to specify which property this shape constrains
        Statement propertyPathStatement = ResourceFactory.createStatement(
            propertyShapeResource,
            ResourceFactory.createProperty(SHACL.PATH.toString()),
            ResourceFactory.createResource(propertyShape.getPath())
        );
        model.add(propertyPathStatement);
        
        // Add rdf:type sh:PropertyShape declaration
        Statement propertyShapeTypeStatement = ResourceFactory.createStatement(
            propertyShapeResource,
            ResourceFactory.createProperty(RDF.type.toString()),
            ResourceFactory.createResource(SHACL.PROPERTY_SHAPE.toString())
        );
        model.add(propertyShapeTypeStatement);
        
        // Handle constraints - either OR-list or simple constraints
        if (propertyShape.getHasOrList()) {
            processOrListConstraints(model, propertyShape);
        } else {
            processSimpleConstraints(model, propertyShape);
        }
    }

    /**
     * Processes OR-list constraints for PropertyShapes with multiple datatype/class options.
     * 
     * This method handles complex PropertyShapes that allow multiple datatypes or classes
     * using SHACL's sh:or construct. It creates an RDF list containing constraint objects
     * for each valid option, filtering out undefined or invalid entries.
     * 
     * The method handles two scenarios:
     * 1. Multiple valid options: Creates sh:or list with constraint objects
     * 2. Single valid option: Applies constraint directly to PropertyShape
     * 
     * @param model The RDF model to add statements to
     * @param propertyShape The PropertyShape containing OR-list constraints
     */
    private void processOrListConstraints(Model model, PS propertyShape) {
        Resource propertyShapeResource = ResourceFactory.createResource(propertyShape.getIri().toString());
        
        // Filter out undefined or null ShaclOrListItems to get clean constraint list
        List<ShaclOrListItem> cleanItems = filterValidOrListItems(propertyShape.getShaclOrListItems());
        
        if (cleanItems.size() > 1) {
            // Multiple options: Create sh:or list with constraint objects
            processMultipleOrListItems(model, propertyShapeResource, cleanItems);
        } else if (cleanItems.size() == 1) {
            // Single option: Apply constraint directly to PropertyShape
            processSingleOrListItem(model, propertyShapeResource, cleanItems.get(0));
        }
        // If no clean items, no constraints are added (graceful handling of empty lists)
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
        List<ShaclOrListItem> cleanItems = new ArrayList<>();
        
        for (ShaclOrListItem item : orListItems) {
            // Include item only if it has a valid datatype/class specification
            if (item.getDataTypeOrClass() != null && !item.getDataTypeOrClass().equals("Undefined")) {
                cleanItems.add(item);
            }
        }
        
        return cleanItems;
    }

    /**
     * Processes multiple OR-list items by creating a SHACL sh:or construct.
     * 
     * This method creates an RDF list containing constraint objects for each
     * valid OR-list item, then links it to the PropertyShape via sh:or property.
     * Each constraint object specifies either sh:class/sh:nodeKind for IRIs
     * or sh:datatype/sh:nodeKind for literals.
     * 
     * @param model The RDF model to add statements to
     * @param propertyShapeResource The PropertyShape resource to attach constraints to
     * @param cleanItems The filtered list of valid ShaclOrListItems
     */
    private void processMultipleOrListItems(Model model, Resource propertyShapeResource, List<ShaclOrListItem> cleanItems) {
        // Create empty RDF list to hold constraint objects
        RDFList constraintList = model.createList(new RDFNode[]{});
        List<Resource> constraintResources = new ArrayList<>();
        
        // Create constraint object for each OR-list item
        for (ShaclOrListItem item : cleanItems) {
            Resource constraintResource = model.createResource();
            addConstraintStatementsForOrListItem(model, constraintResource, item);
            constraintResources.add(constraintResource);
        }
        
        // Add all constraint resources to the RDF list
        for (Resource constraintResource : constraintResources) {
            constraintList = constraintList.with(constraintResource);
        }
        
        // Link the constraint list to the PropertyShape via sh:or
        model.add(
            propertyShapeResource,
            model.createProperty(SHACL.OR.toString()),
            constraintList
        );
    }

    /**
     * Processes a single OR-list item by applying constraints directly to the PropertyShape.
     * 
     * When an OR-list contains only one valid item, the constraints are applied
     * directly to the PropertyShape rather than creating an sh:or construct.
     * This results in cleaner SHACL output for simple cases.
     * 
     * @param model The RDF model to add statements to
     * @param propertyShapeResource The PropertyShape resource to attach constraints to
     * @param item The single ShaclOrListItem to process
     */
    private void processSingleOrListItem(Model model, Resource propertyShapeResource, ShaclOrListItem item) {
        // Add datatype constraint if specified
        if (item.getDataTypeOrClass() != null && !item.getDataTypeOrClass().equals("Undefined")) {
            Statement datatypeStatement = ResourceFactory.createStatement(
                propertyShapeResource,
                ResourceFactory.createProperty(SHACL.DATATYPE.toString()),
                ResourceFactory.createResource(item.getDataTypeOrClass())
            );
            model.add(datatypeStatement);
        }
        
        // Add node kind constraint if specified
        addNodeKindConstraint(model, propertyShapeResource, item.getNodeKind());
    }

    /**
     * Adds constraint statements for a single OR-list item to a constraint resource.
     * 
     * This method determines whether the item represents an IRI or Literal constraint
     * and adds the appropriate SHACL properties:
     * - For IRI: sh:class and sh:nodeKind sh:IRI
     * - For Literal: sh:datatype and sh:nodeKind sh:Literal
     * 
     * @param model The RDF model to add statements to
     * @param constraintResource The constraint resource to add properties to
     * @param item The ShaclOrListItem containing constraint information
     */
    private void addConstraintStatementsForOrListItem(Model model, Resource constraintResource, ShaclOrListItem item) {
        if ("IRI".equals(item.getNodeKind())) {
            // IRI constraint: use sh:class and sh:nodeKind sh:IRI
            Statement nodeKindStatement = ResourceFactory.createStatement(
                constraintResource,
                ResourceFactory.createProperty(SHACL.NODE_KIND.toString()),
                ResourceFactory.createResource(SHACL.IRI.toString())
            );
            Statement classStatement = ResourceFactory.createStatement(
                constraintResource,
                ResourceFactory.createProperty(SHACL.CLASS.toString()),
                ResourceFactory.createResource(item.getDataTypeOrClass())
            );
            model.add(nodeKindStatement);
            model.add(classStatement);
        } else {
            // Literal constraint: use sh:datatype and sh:nodeKind sh:Literal
            Statement nodeKindStatement = ResourceFactory.createStatement(
                constraintResource,
                ResourceFactory.createProperty(SHACL.NODE_KIND.toString()),
                ResourceFactory.createResource(SHACL.LITERAL.toString())
            );
            Statement datatypeStatement = ResourceFactory.createStatement(
                constraintResource,
                ResourceFactory.createProperty(SHACL.DATATYPE.toString()),
                ResourceFactory.createResource(item.getDataTypeOrClass())
            );
            model.add(nodeKindStatement);
            model.add(datatypeStatement);
        }
    }

    /**
     * Processes simple constraints for PropertyShapes without OR-lists.
     * 
     * This method handles PropertyShapes that have direct datatype/class and
     * node kind constraints without complex OR-list structures. It applies
     * constraints directly to the PropertyShape resource.
     * 
     * @param model The RDF model to add statements to
     * @param propertyShape The PropertyShape containing simple constraints
     */
    private void processSimpleConstraints(Model model, PS propertyShape) {
        Resource propertyShapeResource = ResourceFactory.createResource(propertyShape.getIri().toString());
        
        String dataTypeOrClass = propertyShape.getDataTypeOrClass();
        String nodeKind = propertyShape.getNodeKind();
        
        // Add appropriate constraint if specified and not undefined
        if (dataTypeOrClass != null && !dataTypeOrClass.equals("Undefined")) {
            if ("IRI".equals(nodeKind)) {
                // For IRI node kind, use sh:class
                Statement classStatement = ResourceFactory.createStatement(
                    propertyShapeResource,
                    ResourceFactory.createProperty(SHACL.CLASS.toString()),
                    ResourceFactory.createResource(dataTypeOrClass)
                );
                model.add(classStatement);
            } else {
                // For literals or unspecified node kind, use sh:datatype
                Statement datatypeStatement = ResourceFactory.createStatement(
                    propertyShapeResource,
                    ResourceFactory.createProperty(SHACL.DATATYPE.toString()),
                    ResourceFactory.createResource(dataTypeOrClass)
                );
                model.add(datatypeStatement);
            }
        }
        
        // Add node kind constraint if specified
        addNodeKindConstraint(model, propertyShapeResource, nodeKind);
    }

    /**
     * Adds node kind constraint to a resource based on the specified node kind.
     * 
     * This utility method adds sh:nodeKind constraints for both IRI and Literal
     * node kinds. It handles the mapping from string values to SHACL vocabulary terms.
     * 
     * IMPORTANT: This method includes a fix for the QSE-Engine legacy issue where
     * the library returns "NodeKind" instead of the expected "IRI" or "Literal" values.
     * This corrects the hardcoded sh:NodeKind problem described in the issue.
     * 
     * @param model The RDF model to add statements to
     * @param resource The resource to add the node kind constraint to
     * @param nodeKind The node kind string ("IRI", "Literal", or legacy "NodeKind")
     */
    private void addNodeKindConstraint(Model model, Resource resource, String nodeKind) {
        if (nodeKind != null) {
            // Fix for QSE-Engine legacy issue: correct "NodeKind" to "IRI"
            // The QSE library (qse-1.0-QSE-all.jar) sometimes returns "NodeKind" 
            // instead of the expected SHACL-1.0 standard values
            String correctedNodeKind = correctNodeKindValue(nodeKind);
            
            if ("IRI".equals(correctedNodeKind)) {
                Statement nodeKindStatement = ResourceFactory.createStatement(
                    resource,
                    ResourceFactory.createProperty(SHACL.NODE_KIND.toString()),
                    ResourceFactory.createResource(SHACL.IRI.toString())
                );
                model.add(nodeKindStatement);
            } else if ("Literal".equals(correctedNodeKind)) {
                Statement nodeKindStatement = ResourceFactory.createStatement(
                    resource,
                    ResourceFactory.createProperty(SHACL.NODE_KIND.toString()),
                    ResourceFactory.createResource(SHACL.LITERAL.toString())
                );
                model.add(nodeKindStatement);
            }
        }
    }

    /**
     * Corrects legacy NodeKind values from the QSE-Engine.
     * 
     * This method fixes the hardcoded sh:NodeKind problem where the QSE library
     * returns "NodeKind" instead of the proper SHACL-1.0 standard values.
     * As described in the issue analysis, this is a legacy code problem in 
     * qse-1.0-QSE-all.jar with 85% probability.
     * 
     * @param nodeKind The original nodeKind value from QSE library
     * @return The corrected nodeKind value ("IRI" for legacy "NodeKind", otherwise unchanged)
     */
    private String correctNodeKindValue(String nodeKind) {
        if ("NodeKind".equals(nodeKind)) {
            // Legacy fix: QSE-Engine returns "NodeKind" but this should be "IRI"
            // This is the main fix for the issue described in the German analysis
            System.out.println("[DEBUG_LOG] QSE-Engine legacy issue detected: correcting 'NodeKind' -> 'IRI'");
            return "IRI";
        }
        // Return unchanged for proper values ("IRI", "Literal", etc.)
        return nodeKind;
    }

    /**
     * Formats the RDF model as Turtle syntax string.
     * 
     * This method uses the TurtleFormatter to convert the Apache Jena RDF model
     * into a clean, formatted Turtle representation. It maintains compatibility
     * with the existing formatting approach used in the original Utils method.
     * 
     * @param model The RDF model to format
     * @return A string containing the model in Turtle format
     * @throws RuntimeException if formatting fails
     */
    private String formatModelAsTurtle(Model model) {
        try {
            OutputStream outputStream = new ByteArrayOutputStream();
            TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
            formatter.accept(model, outputStream);
            return outputStream.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to format SHACL model as Turtle", e);
        }
    }

    /**
     * Returns the format name for this formatter.
     * 
     * @return "SHACL" as the format identifier
     */
    @Override
    public String getFormatName() {
        return "SHACL";
    }

    /**
     * Returns the file extension for SHACL files.
     * 
     * @return "ttl" as SHACL is typically serialized in Turtle format
     */
    @Override
    public String getFileExtension() {
        return "ttl";
    }

    /**
     * Validates that the provided NodeShapes can be formatted as SHACL.
     * 
     * This method performs basic validation to ensure the input NodeShapes
     * contain the necessary information for SHACL generation. It checks for
     * null values and validates that NodeShapes have required properties.
     * 
     * @param nodeShapes The set of NodeShapes to validate
     * @return true if the NodeShapes can be formatted as SHACL, false otherwise
     * @throws IllegalArgumentException if nodeShapes is null
     */
    @Override
    public boolean canFormat(Set<NS> nodeShapes) {
        if (nodeShapes == null) {
            throw new IllegalArgumentException("NodeShapes cannot be null");
        }
        
        // Empty sets are valid (will produce empty SHACL output)
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