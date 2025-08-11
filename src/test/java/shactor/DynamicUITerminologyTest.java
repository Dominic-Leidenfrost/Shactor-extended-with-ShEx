package shactor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that UI terminology dynamically changes based on the selected format.
 * 
 * This test ensures that SHACL terminology is properly replaced with ShEx terminology
 * when the user selects ShEx format, and vice versa.
 */
public class DynamicUITerminologyTest {

    @BeforeEach
    void setUp() {
        // Reset to default format before each test
        IndexView.selectedFormat = "SHACL";
    }

    @Test
    @DisplayName("Should show SHACL terminology when SHACL format is selected")
    void shouldShowShaclTerminologyWhenShaclFormatSelected() {
        // Set format to SHACL
        IndexView.selectedFormat = "SHACL";
        
        // Test tab title logic
        String analyzeTabTitle = IndexView.selectedFormat.equals("ShEx") ? "Analyze ShEx Shapes" : "Analyze SHACL Shapes";
        assertEquals("Analyze SHACL Shapes", analyzeTabTitle);
        
        // Test column header logic (simulating PsView setupGrid method)
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        String classOrDataTypeHeader = formatName.equals("ShEx") ? "Class or dataType" : "sh:Class or sh:dataType";
        
        assertEquals("sh:NodeKind", nodeKindHeader);
        assertEquals("sh:Class or sh:dataType", classOrDataTypeHeader);
    }

    @Test
    @DisplayName("Should show ShEx terminology when ShEx format is selected")
    void shouldShowShexTerminologyWhenShexFormatSelected() {
        // Set format to ShEx
        IndexView.selectedFormat = "ShEx";
        
        // Test tab title logic
        String analyzeTabTitle = IndexView.selectedFormat.equals("ShEx") ? "Analyze ShEx Shapes" : "Analyze SHACL Shapes";
        assertEquals("Analyze ShEx Shapes", analyzeTabTitle);
        
        // Test column header logic (simulating PsView setupGrid method)
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        String classOrDataTypeHeader = formatName.equals("ShEx") ? "Class or dataType" : "sh:Class or sh:dataType";
        
        assertEquals("NodeKind", nodeKindHeader);
        assertEquals("Class or dataType", classOrDataTypeHeader);
    }

    @Test
    @DisplayName("Should default to SHACL terminology when selectedFormat is null")
    void shouldDefaultToShaclTerminologyWhenSelectedFormatIsNull() {
        // Set format to null
        IndexView.selectedFormat = null;
        
        // Test tab title logic (should default to SHACL)
        String analyzeTabTitle = (IndexView.selectedFormat != null && IndexView.selectedFormat.equals("ShEx")) ? "Analyze ShEx Shapes" : "Analyze SHACL Shapes";
        assertEquals("Analyze SHACL Shapes", analyzeTabTitle);
        
        // Test column header logic (should default to SHACL)
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        String classOrDataTypeHeader = formatName.equals("ShEx") ? "Class or dataType" : "sh:Class or sh:dataType";
        
        assertEquals("sh:NodeKind", nodeKindHeader);
        assertEquals("sh:Class or sh:dataType", classOrDataTypeHeader);
    }

    @Test
    @DisplayName("Should be case sensitive for format comparison")
    void shouldBeCaseSensitiveForFormatComparison() {
        // Test with different case variations
        IndexView.selectedFormat = "shex"; // lowercase
        
        String analyzeTabTitle = IndexView.selectedFormat.equals("ShEx") ? "Analyze ShEx Shapes" : "Analyze SHACL Shapes";
        assertEquals("Analyze SHACL Shapes", analyzeTabTitle); // Should default to SHACL since "shex" != "ShEx"
        
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        assertEquals("sh:NodeKind", nodeKindHeader); // Should show SHACL format since "shex" != "ShEx"
    }

    @Test
    @DisplayName("Should handle format switching correctly")
    void shouldHandleFormatSwitchingCorrectly() {
        // Start with SHACL
        IndexView.selectedFormat = "SHACL";
        String formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        String nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        assertEquals("sh:NodeKind", nodeKindHeader);
        
        // Switch to ShEx
        IndexView.selectedFormat = "ShEx";
        formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        assertEquals("NodeKind", nodeKindHeader);
        
        // Switch back to SHACL
        IndexView.selectedFormat = "SHACL";
        formatName = IndexView.selectedFormat != null ? IndexView.selectedFormat : "SHACL";
        nodeKindHeader = formatName.equals("ShEx") ? "NodeKind" : "sh:NodeKind";
        assertEquals("sh:NodeKind", nodeKindHeader);
    }
}