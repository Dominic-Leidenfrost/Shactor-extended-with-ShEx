package shactor.utils.formatters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating and managing ShapeFormatter implementations.
 * 
 * This factory provides a centralized way to obtain ShapeFormatter instances
 * based on format names. It supports both SHACL and ShEx formatters and can
 * be easily extended to support additional shape expression formats in the future.
 * 
 * The factory uses Spring's dependency injection to automatically discover
 * and register all available ShapeFormatter implementations, making it
 * extensible without code changes when new formatters are added.
 * 
 * Usage examples:
 * ```java
 * // Get SHACL formatter
 * ShapeFormatter shaclFormatter = factory.getFormatter("SHACL");
 * 
 * // Get ShEx formatter  
 * ShapeFormatter shexFormatter = factory.getFormatter("ShEx");
 * 
 * // Check if format is supported
 * boolean isSupported = factory.isFormatSupported("SHACL");
 * ```
 * 
 * @author ShEx Integration Implementation - Phase 4
 * @version 1.0
 * @since Phase 4.1
 */
@Component
public class ShapeFormatterFactory {

    /**
     * Map of format names to their corresponding formatter instances.
     * This map is populated automatically by Spring dependency injection.
     */
    private final Map<String, ShapeFormatter> formatters;

    /**
     * Constructor that initializes the factory with available formatters.
     * 
     * Spring automatically injects all ShapeFormatter implementations
     * and this constructor registers them by their format names for
     * easy lookup and retrieval.
     * 
     * @param shaclFormatter The SHACL formatter implementation
     * @param shexFormatter The ShEx formatter implementation
     */
    @Autowired
    public ShapeFormatterFactory(ShaclFormatter shaclFormatter, ShExFormatter shexFormatter) {
        this.formatters = new HashMap<>();
        
        // Register SHACL formatter
        registerFormatter(shaclFormatter);
        
        // Register ShEx formatter
        registerFormatter(shexFormatter);
    }

    /**
     * Registers a formatter instance with the factory.
     * 
     * This method adds a formatter to the internal registry using its
     * format name as the key. This allows for dynamic registration of
     * formatters and makes the factory extensible.
     * 
     * @param formatter The ShapeFormatter implementation to register
     * @throws IllegalArgumentException if formatter is null or has invalid format name
     */
    private void registerFormatter(ShapeFormatter formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("Formatter cannot be null");
        }
        
        String formatName = formatter.getFormatName();
        if (formatName == null || formatName.trim().isEmpty()) {
            throw new IllegalArgumentException("Formatter must have a valid format name");
        }
        
        formatters.put(formatName.toUpperCase(), formatter);
    }

    /**
     * Retrieves a formatter instance for the specified format.
     * 
     * This method looks up and returns the appropriate ShapeFormatter
     * implementation based on the provided format name. The lookup is
     * case-insensitive for user convenience.
     * 
     * Supported formats:
     * - "SHACL" - Returns ShaclFormatter for SHACL Turtle output
     * - "ShEx" - Returns ShExFormatter for ShEx syntax output
     * 
     * @param formatName The name of the desired format (case-insensitive)
     * @return The ShapeFormatter implementation for the specified format
     * @throws IllegalArgumentException if formatName is null, empty, or unsupported
     */
    public ShapeFormatter getFormatter(String formatName) {
        // Validate input parameter
        if (formatName == null || formatName.trim().isEmpty()) {
            throw new IllegalArgumentException("Format name cannot be null or empty");
        }
        
        // Normalize format name to uppercase for case-insensitive lookup
        String normalizedFormatName = formatName.trim().toUpperCase();
        
        // Look up formatter in registry
        ShapeFormatter formatter = formatters.get(normalizedFormatName);
        
        if (formatter == null) {
            throw new IllegalArgumentException("Unsupported format: " + formatName + 
                ". Supported formats: " + String.join(", ", getSupportedFormats()));
        }
        
        return formatter;
    }

    /**
     * Checks if a specific format is supported by this factory.
     * 
     * This method provides a way to validate format names before
     * attempting to retrieve formatters, allowing for graceful
     * error handling in client code.
     * 
     * @param formatName The format name to check (case-insensitive)
     * @return true if the format is supported, false otherwise
     */
    public boolean isFormatSupported(String formatName) {
        if (formatName == null || formatName.trim().isEmpty()) {
            return false;
        }
        
        String normalizedFormatName = formatName.trim().toUpperCase();
        return formatters.containsKey(normalizedFormatName);
    }

    /**
     * Returns an array of all supported format names.
     * 
     * This method provides a way for client code to discover which
     * formats are available without having to catch exceptions or
     * guess format names.
     * 
     * @return Array of supported format names in uppercase
     */
    public String[] getSupportedFormats() {
        return formatters.keySet().toArray(new String[0]);
    }

    /**
     * Returns the number of registered formatters.
     * 
     * This method is primarily useful for testing and debugging
     * to verify that all expected formatters have been registered.
     * 
     * @return The number of registered formatter implementations
     */
    public int getFormatterCount() {
        return formatters.size();
    }

    /**
     * Gets the default formatter (SHACL) for backward compatibility.
     * 
     * This method provides a default formatter when no specific format
     * is requested, maintaining compatibility with existing code that
     * doesn't specify a format preference.
     * 
     * @return The default ShapeFormatter (SHACL)
     * @throws RuntimeException if SHACL formatter is not available
     */
    public ShapeFormatter getDefaultFormatter() {
        ShapeFormatter defaultFormatter = formatters.get("SHACL");
        if (defaultFormatter == null) {
            throw new RuntimeException("Default SHACL formatter is not available");
        }
        return defaultFormatter;
    }

    /**
     * Formats shapes using the specified format.
     * 
     * This is a convenience method that combines formatter lookup and
     * shape formatting in a single call. It's equivalent to calling
     * getFormatter(formatName).formatShapes(nodeShapes) but provides
     * better error handling and logging.
     * 
     * @param nodeShapes The set of NodeShapes to format
     * @param formatName The desired output format
     * @return Formatted shapes as a string
     * @throws IllegalArgumentException if parameters are invalid or format is unsupported
     */
    public String formatShapes(java.util.Set<cs.qse.common.structure.NS> nodeShapes, String formatName) {
        // Validate input parameters
        if (nodeShapes == null) {
            throw new IllegalArgumentException("NodeShapes cannot be null");
        }
        
        // Get the appropriate formatter and format the shapes
        ShapeFormatter formatter = getFormatter(formatName);
        return formatter.formatShapes(nodeShapes);
    }
}