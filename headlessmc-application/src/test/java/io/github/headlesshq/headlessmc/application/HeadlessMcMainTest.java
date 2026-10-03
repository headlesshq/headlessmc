package io.github.headlesshq.headlessmc.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HeadlessMcMainTest {
    private static final String KEY = "hmc.test.parse.key";
    private static final String OTHER = "hmc.test.parse.other";
    private static final String EMPTY = "hmc.test.parse.empty";
    private static final String JVM = "hmc.test.parse.jvm";

    @AfterEach
    public void clearProperties() {
        System.clearProperty(KEY);
        System.clearProperty(OTHER);
        System.clearProperty(EMPTY);
        System.clearProperty(JVM);
    }

    @Test
    public void testLeadingSystemPropertiesAreParsedAndRemoved() {
        String[] result = HeadlessMcMain.parseSystemProperties(new String[]{
            "-D" + KEY + "=value", "-D" + OTHER + "=a=b", "-D" + EMPTY, "launch", "26.1"
        });

        assertArrayEquals(new String[]{"launch", "26.1"}, result);
        assertEquals("value", System.getProperty(KEY));
        assertEquals("a=b", System.getProperty(OTHER));
        assertEquals("", System.getProperty(EMPTY));
    }

    @Test
    public void testSystemPropertiesAfterFirstArgAreKept() {
        // https://github.com/headlesshq/headlessmc/issues/420
        String[] result = HeadlessMcMain.parseSystemProperties(new String[]{
            "-D" + KEY + "=value", "launch", "--jvm", "-D" + JVM + "=value", "26.1"
        });

        assertArrayEquals(new String[]{"launch", "--jvm", "-D" + JVM + "=value", "26.1"}, result);
        assertEquals("value", System.getProperty(KEY));
        assertNull(System.getProperty(JVM));
    }

    @Test
    public void testNoSystemProperties() {
        String[] args = {"launch", "26.1"};
        assertArrayEquals(args, HeadlessMcMain.parseSystemProperties(args));
        assertArrayEquals(new String[0], HeadlessMcMain.parseSystemProperties(new String[0]));
    }

    @Test
    public void testOnlySystemProperties() {
        String[] result = HeadlessMcMain.parseSystemProperties(new String[]{"-D" + KEY + "=value"});
        assertArrayEquals(new String[0], result);
        assertEquals("value", System.getProperty(KEY));
    }

}
