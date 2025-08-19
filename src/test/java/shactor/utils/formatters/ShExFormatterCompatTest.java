package shactor.utils.formatters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Consolidated into ShExFormatterTest; keeping file for history")
@DisplayName("ShExC compatibility helper tests")
class ShExFormatterCompatTest {

    @Test
    @DisplayName("disjunction emits ( A OR B ) and never pipes")
    void disjunction_no_pipes() {
        String out = ShExFormatter.disjunction(List.of("@ex:AShape", "@ex:BShape"));
        assertEquals("( @ex:AShape OR @ex:BShape )", out);
        assertFalse(out.contains("|"), "Output must not contain pipe '|' characters");
    }

    @Test
    @DisplayName("emitValueSet maps sh:in to [ v1 v2 ] with clean quoting")
    void emitValueSet_basic() {
        String out = ShExFormatter.emitValueSet(List.of("xsd:string", "ex:Foo", "bar"));
        assertEquals("[xsd:string ex:Foo \"bar\"]", out);
    }

    @Test
    @DisplayName("formatCardinality maps common counts correctly")
    void formatCardinality_mappings() {
        assertEquals("?", ShExFormatter.formatCardinality(0, 1));
        assertEquals("+", ShExFormatter.formatCardinality(1, null));
        assertEquals("*", ShExFormatter.formatCardinality(0, null));
        assertEquals("{2,5}", ShExFormatter.formatCardinality(2, 5));
        assertEquals("", ShExFormatter.formatCardinality(1, 1));
        assertEquals("{3,}", ShExFormatter.formatCardinality(3, -1));
    }

    @Test
    @DisplayName("formatPath supports inverse path with ^pred")
    void formatPath_inverse() {
        String out = ShExFormatter.formatPath("^http://example.org/knows");
        assertEquals("^ex:knows", out);
    }

    @Test
    @DisplayName("emitClassShape produces helper shape with rdf:type value set")
    void emitClassShape_basic() {
        String out = ShExFormatter.emitClassShape("ex:Person", "ex:PersonShape");
        assertEquals("ex:PersonShape { rdf:type [ex:Person] + }\n\n", out);
    }

    @Test
    @DisplayName("Prefixes include ub: and CURIE mapping works for UB IRIs")
    void prefixes_and_ub_mapping() {
        ShExFormatter f = new ShExFormatter();
        String out = f.formatShapes(new java.util.HashSet<>());
        assertTrue(out.contains("PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>"));
        String path = ShExFormatter.formatPath("http://swat.cse.lehigh.edu/onto/univ-bench.owl#memberOf");
        assertEquals("ub:memberOf", path);
    }
}
