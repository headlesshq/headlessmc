package io.github.headlesshq.headlessmc.version.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateStringTest {
    @Test
    public void testProcessing() {
        TemplateStrings templateStrings = new TemplateStrings();
        templateStrings.add(TemplateString.ARCH, "64");
        templateStrings.add(TemplateString.CLIENT_ID, "hello");

        String preProcessedString = "test ${arch} test ${clientid}";
        String result = templateStrings.process(preProcessedString);
        assertEquals("test 64 test hello", result);
        assertTrue(templateStrings.hasProcessed(TemplateString.ARCH));
        assertTrue(templateStrings.hasProcessed(TemplateString.CLIENT_ID));
    }

    @Test
    public void testFailedProcessing() {
        TemplateStrings templateStrings = new TemplateStrings();
        templateStrings.add(TemplateString.ARCH, "64");

        String preProcessedString = "test ${arch} test ${clientid}";
        String result = templateStrings.process(preProcessedString);
        assertEquals("test 64 test ", result);
        assertTrue(templateStrings.getFailed().contains("${clientid}"));
        assertTrue(templateStrings.hasProcessed(TemplateString.ARCH));
    }

}
