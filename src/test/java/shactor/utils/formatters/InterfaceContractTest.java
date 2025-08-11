package shactor.utils.formatters;

import cs.qse.common.structure.NS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic interface contract tests for ShapeFormatter implementations.
 * 
 * These tests verify that the ShapeFormatter interface contract
 * is properly defined and can be implemented correctly.
 */
@DisplayName("ShapeFormatter Interface Contract Tests")
class InterfaceContractTest {

    private Set<NS> sampleNodeShapes;
    private ShapeFormatter mockFormatter;

    /**
     * Set up test data and mock objects before each test.
     */
    @BeforeEach
    void setUp() {
        // Initialize empty set for basic testing
        sampleNodeShapes = new HashSet<>();
        
        // Create a simple mock implementation for basic testing
        mockFormatter = new ShapeFormatter() {
            @Override
            public String formatShapes(Set<NS> nodeShapes) {
                if (nodeShapes == null) {
                    throw new IllegalArgumentException("NodeShapes cannot be null");
                }
                return "Mock formatted output";
            }

            @Override
            public String getFormatName() {
                return "Mock";
            }

            @Override
            public String getFileExtension() {
                return "mock";
            }

            @Override
            public boolean canFormat(Set<NS> nodeShapes) {
                if (nodeShapes == null) {
                    throw new IllegalArgumentException("NodeShapes cannot be null");
                }
                return true;
            }
        };
    }

    @Test
    @DisplayName("Should have formatShapes method")
    void shouldHaveFormatShapesMethod() {
        // Test that the interface method exists and can be called
        assertDoesNotThrow(() -> {
            String result = mockFormatter.formatShapes(sampleNodeShapes);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("Should have getFormatName method")
    void shouldHaveGetFormatNameMethod() {
        // Test that format name can be retrieved
        String formatName = mockFormatter.getFormatName();
        assertNotNull(formatName);
        assertFalse(formatName.trim().isEmpty());
    }

    @Test
    @DisplayName("Should have getFileExtension method")
    void shouldHaveGetFileExtensionMethod() {
        // Test that file extension can be retrieved
        String extension = mockFormatter.getFileExtension();
        assertNotNull(extension);
        assertFalse(extension.trim().isEmpty());
    }

    @Test
    @DisplayName("Should have canFormat method")
    void shouldHaveCanFormatMethod() {
        // Test that validation method exists
        assertDoesNotThrow(() -> {
            boolean canFormat = mockFormatter.canFormat(sampleNodeShapes);
            // Should return true for empty but non-null set
            assertTrue(canFormat);
        });
    }
}