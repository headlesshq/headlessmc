package io.github.headlesshq.headlessmc.java.version;

import io.github.headlesshq.headlessmc.java.Java;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class SpecificationVersionParserTest {
    @Inject
    SpecificationVersionParser specificationVersionParser;

    @Test
    public void testSpecificationVersionParser() {
        assertEquals(25, specificationVersionParser.parseSpecificationVersion("25"));
        assertEquals(21, specificationVersionParser.parseSpecificationVersion("21"));
        assertEquals(17, specificationVersionParser.parseSpecificationVersion("17"));
        assertEquals(8, specificationVersionParser.parseSpecificationVersion("8"));
        assertEquals(8, specificationVersionParser.parseSpecificationVersion("1.8"));
        assertEquals(6, specificationVersionParser.parseSpecificationVersion("1.6"));
        assertEquals(Java.JAVA_VERSION_0_9, specificationVersionParser.parseSpecificationVersion("0.9"));
    }

}
