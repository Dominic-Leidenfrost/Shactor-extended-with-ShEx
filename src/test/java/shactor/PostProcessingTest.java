package shactor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the post-processing logic for turtle content.
 * 
 * This test simulates the postProcessTurtleContent method functionality
 * to ensure the quick and dirty fixes work as expected.
 */
@DisplayName("Post-Processing Tests")
class PostProcessingTest {

    /**
     * Simulates the postProcessTurtleContent method for testing purposes.
     */
    private String postProcessTurtleContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // 1. Fix NodeKind casing: replace "NodeKind" with "nodeKind" 
        content = content.replace("NodeKind", "nodeKind");
        
        // 2. Fix decimal comma in confidence values and add xsd:double datatype
        // Pattern matches: qse:confidence followed by whitespace, then number with comma, then optional whitespace and semicolon
        // Example: "qse:confidence 1,2E-1 ;" becomes "qse:confidence \"1.2E-1\"^^xsd:double ;"
        content = content.replaceAll(
            "(qse:confidence\\s+)([0-9]+,[0-9]+(?:E[+-]?[0-9]+)?)\\s*;",
            "$1\"" + "$2" + "\"^^xsd:double ;"
        );
        
        // 3. Replace commas with dots in the matched confidence values
        // This needs to be done after the datatype annotation to avoid affecting other commas
        content = content.replaceAll(
            "(qse:confidence\\s+\"[^\"]*),([^\"]*\"\\^\\^xsd:double)",
            "$1.$2"
        );
        
        // 4. Fix untyped scientific notation values like "1E0" → "1.0E0"^^xsd:double
        // Pattern matches: qse:confidence followed by whitespace, then scientific notation without decimal point
        // Example: "qse:confidence 1E0 ;" becomes "qse:confidence \"1.0E0\"^^xsd:double ;"
        content = content.replaceAll(
            "(qse:confidence\\s+)([0-9]+E[+-]?[0-9]+)\\s*;",
            "$1\"$2\"^^xsd:double ;"
        );
        
        // 5. Add decimal point to scientific notation values that lack it (inside quotes)
        // This converts "1E0" to "1.0E0" within the already quoted and typed values
        content = content.replaceAll(
            "(qse:confidence\\s+\")([0-9]+)(E[+-]?[0-9]+)(\"\\^\\^xsd:double)",
            "$1$2.0$3$4"
        );
        
        return content;
    }

    @Test
    @DisplayName("Should replace NodeKind with nodeKind")
    void shouldReplaceNodeKindCasing() {
        String input = "<http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;";
        String expected = "<http://www.w3.org/ns/shacl#nodeKind> <http://www.w3.org/ns/shacl#IRI> ;";
        
        String result = postProcessTurtleContent(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should fix decimal comma in confidence values and add xsd:double")
    void shouldFixDecimalCommaInConfidence() {
        String input = "<http://shaclshapes.org/confidence> 1,2E-1 ;";
        String expected = "<http://shaclshapes.org/confidence> \"1.2E-1\"^^xsd:double ;";
        
        String result = postProcessTurtleContent(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle multiple confidence values")
    void shouldHandleMultipleConfidenceValues() {
        String input = "qse:confidence 7,1332E-2 ;\nqse:confidence 6,643E-2 ;\nqse:confidence 1,2E-1 ;";
        String expected = "qse:confidence \"7.1332E-2\"^^xsd:double ;\nqse:confidence \"6.643E-2\"^^xsd:double ;\nqse:confidence \"1.2E-1\"^^xsd:double ;";
        
        String result = postProcessTurtleContent(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle combined NodeKind and confidence fixes")
    void shouldHandleCombinedFixes() {
        String input = """
            <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;
            <http://shaclshapes.org/confidence> 7,1332E-2 ;
            <http://www.w3.org/ns/shacl#NodeKind> <http://www.w3.org/ns/shacl#IRI> ;
            qse:confidence 1,2E-1 ;
            """;
        
        String expected = """
            <http://www.w3.org/ns/shacl#nodeKind> <http://www.w3.org/ns/shacl#IRI> ;
            <http://shaclshapes.org/confidence> "7.1332E-2"^^xsd:double ;
            <http://www.w3.org/ns/shacl#nodeKind> <http://www.w3.org/ns/shacl#IRI> ;
            qse:confidence "1.2E-1"^^xsd:double ;
            """;
        
        String result = postProcessTurtleContent(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle null and empty content gracefully")
    void shouldHandleNullAndEmptyContent() {
        assertNull(postProcessTurtleContent(null));
        assertEquals("", postProcessTurtleContent(""));
        assertEquals("   ", postProcessTurtleContent("   "));
    }

    @Test
    @DisplayName("Should fix untyped scientific notation values like 1E0")
    void shouldFixUntypedScientificNotation() {
        String input = "qse:confidence 1E0 ;";
        String expected = "qse:confidence \"1.0E0\"^^xsd:double ;";
        
        String result = postProcessTurtleContent(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle multiple scientific notation patterns")
    void shouldHandleMultipleScientificNotationPatterns() {
        String input = "qse:confidence 2E-1 ;\nqse:confidence 5E+2 ;\nqse:confidence 1E0 ;";
        String expected = "qse:confidence \"2.0E-1\"^^xsd:double ;\nqse:confidence \"5.0E+2\"^^xsd:double ;\nqse:confidence \"1.0E0\"^^xsd:double ;";
        
        String result = postProcessTurtleContent(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle scientific notation in sh:or blocks")
    void shouldHandleScientificNotationInOrBlocks() {
        String input = """
            qse:doctoralDegreeFromFullProfessorShapeProperty a sh:PropertyShape ;
              qse:confidence 1E0 ;
              qse:support "125"^^xsd:int ;
              sh:nodeKind sh:IRI ;
              sh:class <http://swat.cse.lehigh.edu/onto/univ-bench.owl#University> ;
              sh:path <http://swat.cse.lehigh.edu/onto/univ-bench.owl#doctoralDegreeFrom> .
            """;
        
        String expected = """
            qse:doctoralDegreeFromFullProfessorShapeProperty a sh:PropertyShape ;
              qse:confidence "1.0E0"^^xsd:double ;
              qse:support "125"^^xsd:int ;
              sh:nodeKind sh:IRI ;
              sh:class <http://swat.cse.lehigh.edu/onto/univ-bench.owl#University> ;
              sh:path <http://swat.cse.lehigh.edu/onto/univ-bench.owl#doctoralDegreeFrom> .
            """;
        
        String result = postProcessTurtleContent(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should not affect content without target patterns")
    void shouldNotAffectContentWithoutTargetPatterns() {
        String input = """
            @prefix sh: <http://www.w3.org/ns/shacl#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            
            :SomeShape a sh:NodeShape ;
                sh:targetClass :SomeClass ;
                sh:property :SomeProperty .
            """;
        
        String result = postProcessTurtleContent(input);
        assertEquals(input, result);
    }
}