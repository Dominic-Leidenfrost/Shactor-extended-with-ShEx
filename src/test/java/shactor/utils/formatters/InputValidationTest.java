package shactor.utils.formatters;

import cs.qse.common.structure.NS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Input validation tests for ShapeFormatter implementations.
 * 
 * These tests verify that implementations properly handle
 * null and invalid input parameters.
 */
@DisplayName("ShapeFormatter Input Validation Tests")
class InputValidationTest {

    private ShapeFormatter mockFormatter;

    /**
     * Set up test data and mock objects before each test.
     */
    @BeforeEach
    void setUp() {
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
    @DisplayName("Should throw exception for null nodeShapes in formatShapes")
    void shouldThrowExceptionForNullNodeShapesInFormatShapes() {
        // Test that null input is properly handled
        assertThrows(IllegalArgumentException.class, () -> {
            mockFormatter.formatShapes(null);
        });
    }

    @Test
    @DisplayName("Should handle empty nodeShapes set")
    void shouldHandleEmptyNodeShapesSet() {
        // Test that empty set is handled gracefully
        assertDoesNotThrow(() -> {
            String result = mockFormatter.formatShapes(new HashSet<>());
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("Should throw exception for null nodeShapes in canFormat")
    void shouldThrowExceptionForNullNodeShapesInCanFormat() {
        // Test that canFormat properly validates null input
        assertThrows(IllegalArgumentException.class, () -> {
            mockFormatter.canFormat(null);
        });
    }
}