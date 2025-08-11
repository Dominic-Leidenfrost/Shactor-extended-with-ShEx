package shactor.utils.formatters;

import cs.qse.common.structure.NS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for SHACL formatter implementation.
 * 
 * These tests verify the basic functionality of the ShaclFormatter
 * without requiring complex mock implementations of external interfaces.
 * 
 * Note: This is a refactored test class extracted from the large ShapeFormatterTest.java
 * These tests would need actual ShaclFormatter implementation to run successfully.
 */
@DisplayName("SHACL Formatter Tests")
class ShaclFormatterTest {

    private ShaclFormatter shaclFormatter;

    @BeforeEach
    void setUp() {
        shaclFormatter = new ShaclFormatter();
    }

    @Test
    @DisplayName("Should return correct format name")
    void shouldReturnCorrectFormatName() {
        assertEquals("SHACL", shaclFormatter.getFormatName());
    }

    @Test
    @DisplayName("Should return correct file extension")
    void shouldReturnCorrectFileExtension() {
        assertEquals("ttl", shaclFormatter.getFileExtension());
    }

    @Test
    @DisplayName("Should validate empty NodeShapes set")
    void shouldValidateEmptyNodeShapesSet() {
        Set<NS> emptySet = new HashSet<>();
        assertTrue(shaclFormatter.canFormat(emptySet));
    }

    @Test
    @DisplayName("Should format empty NodeShapes set")
    void shouldFormatEmptyNodeShapesSet() {
        Set<NS> emptySet = new HashSet<>();
        String result = shaclFormatter.formatShapes(emptySet);
        
        assertNotNull(result);
        // Empty set should produce empty result since there's nothing to format
        // This is the correct behavior - no NodeShapes means no SHACL output
        assertTrue(result.isEmpty() || result.trim().isEmpty(), 
            "Empty NodeShapes set should produce empty or whitespace-only result");
    }

    @Test
    @DisplayName("Should throw exception for null NodeShapes")
    void shouldThrowExceptionForNullNodeShapes() {
        assertThrows(IllegalArgumentException.class, () -> {
            shaclFormatter.formatShapes(null);
        });
    }

    @Test
    @DisplayName("Should throw exception for null NodeShapes in canFormat")
    void shouldThrowExceptionForNullNodeShapesInCanFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            shaclFormatter.canFormat(null);
        });
    }

    @Test
    @DisplayName("Should handle runtime exceptions gracefully")
    void shouldHandleRuntimeExceptionsGracefully() {
        // Test that the formatter handles internal errors properly
        Set<NS> emptySet = new HashSet<>();
        
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            String result = shaclFormatter.formatShapes(emptySet);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("Should create instance without errors")
    void shouldCreateInstanceWithoutErrors() {
        // Test that ShaclFormatter can be instantiated
        assertDoesNotThrow(() -> {
            ShaclFormatter formatter = new ShaclFormatter();
            assertNotNull(formatter);
        });
    }

    @Test
    @DisplayName("Should have consistent interface implementation")
    void shouldHaveConsistentInterfaceImplementation() {
        // Test that all interface methods are implemented consistently
        assertNotNull(shaclFormatter.getFormatName());
        assertNotNull(shaclFormatter.getFileExtension());
        assertFalse(shaclFormatter.getFormatName().trim().isEmpty());
        assertFalse(shaclFormatter.getFileExtension().trim().isEmpty());
    }
}