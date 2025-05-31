package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonArray;
import io.github.headlesshq.headlessmc.version.ExtractionRules;
import io.github.headlesshq.headlessmc.version.Extractor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class ExtractorParserTest implements UsesResources {
    @Test
    @SuppressWarnings("ConstantValue")
    public void testExtractorFactory() {
        ExtractorParser factory = new ExtractorParser();
        ExtractionRules extractor = factory.parse(null);
        assertNull(extractor);

        Assertions.assertEquals(Extractor.NO_EXTRACTION, Extractor.of(extractor));
        extractor = factory.parse(new JsonArray());
        Assertions.assertEquals(Extractor.NO_EXTRACTION, Extractor.of(extractor));
        extractor = factory.parse(getJsonObject("extractor.json"));
        Assertions.assertNotEquals(Extractor.NO_EXTRACTION, Extractor.of(extractor));

        Assertions.assertTrue(Extractor.of(extractor).isExtracting());
        Assertions.assertTrue(Extractor.of(extractor).shouldExtract("test"));
        Assertions.assertFalse(Extractor.of(extractor).shouldExtract("META-INF/"));
        Assertions.assertFalse(Extractor.of(extractor).shouldExtract("META-INF/test"));

        Assertions.assertFalse(Extractor.NO_EXTRACTION.isExtracting());
        Assertions.assertFalse(Extractor.NO_EXTRACTION.shouldExtract("test"));
    }

}
