package shactor.utils.formatters;

import cs.qse.common.structure.NS;
import java.util.Set;

/**
 * Interface for formatting shape expressions in different formats.
 * 
 * This interface provides a contract for implementing various shape expression
 * formatters such as SHACL (Shapes Constraint Language) and ShEx (Shape Expressions).
 * Each implementation should handle the conversion of NodeShapes and their associated
 * PropertyShapes into the appropriate format-specific syntax.
 * 
 * The interface is designed to support the existing SHACL functionality while
 * enabling future extensions for ShEx and other shape expression languages.
 * 
 * @author ShEx Integration Implementation
 * @version 1.0
 * @since Phase 1.2
 */
public interface ShapeFormatter {
    
    /**
     * Formats a set of NodeShapes and their PropertyShapes into the target format.
     * 
     * This method takes a collection of NodeShapes (NS) objects, each containing
     * their associated PropertyShapes (PS), and converts them into a string
     * representation in the specific format implemented by the concrete class.
     * 
     * The method should handle:
     * - Basic node shape definitions with target classes
     * - Property shape constraints including datatypes and node kinds
     * - OR-list constraints (ShaclOrListItem structures)
     * - IRI and Literal node kinds
     * - Proper namespace declarations and prefixes
     * 
     * @param nodeShapes A set of NodeShape objects containing the shapes to format.
     *                   Each NodeShape includes its IRI, target class, and associated
     *                   PropertyShapes with their constraints.
     * @return A string representation of the shapes in the target format.
     *         For SHACL implementations, this would be Turtle syntax.
     *         For ShEx implementations, this would be ShEx syntax.
     * @throws IllegalArgumentException if nodeShapes is null or contains invalid data
     * @throws RuntimeException if formatting fails due to internal errors
     */
    String formatShapes(Set<NS> nodeShapes);
    
    /**
     * Returns the format name supported by this formatter.
     * 
     * This method provides a human-readable identifier for the format
     * that this formatter produces. It can be used for logging, debugging,
     * and format selection in user interfaces.
     * 
     * @return A string identifying the format (e.g., "SHACL", "ShEx")
     */
    String getFormatName();
    
    /**
     * Returns the file extension typically used for files in this format.
     * 
     * This method provides the standard file extension for the format,
     * which can be used when saving formatted output to files.
     * 
     * @return A string representing the file extension without the dot
     *         (e.g., "ttl" for Turtle, "shex" for ShEx)
     */
    String getFileExtension();
    
    /**
     * Validates that the provided NodeShapes can be formatted by this formatter.
     * 
     * This method performs preliminary validation to ensure that the input
     * NodeShapes contain the necessary information and are in a valid state
     * for formatting. It should check for null values, required fields,
     * and format-specific constraints.
     * 
     * @param nodeShapes The set of NodeShapes to validate
     * @return true if the NodeShapes can be formatted, false otherwise
     * @throws IllegalArgumentException if nodeShapes is null
     */
    boolean canFormat(Set<NS> nodeShapes);
}