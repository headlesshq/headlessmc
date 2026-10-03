package io.github.headlesshq.headlessmc.java.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SystemPropertyUtilTest {
    @Test
    public void testIsSystemProperty() {
        assertTrue(SystemPropertyUtil.isSystemProperty("-Dhmc.java=false"));
        assertTrue(SystemPropertyUtil.isSystemProperty("-Dtest=value"));
        assertTrue(SystemPropertyUtil.isSystemProperty("-Dtest"));
        assertFalse(SystemPropertyUtil.isSystemProperty("Dtest"));
        assertFalse(SystemPropertyUtil.isSystemProperty("test"));
        assertFalse(SystemPropertyUtil.isSystemProperty(""));
    }

    @Test
    public void testParseSystemProperty() {
        ArgPair pair = SystemPropertyUtil.parseSystemProperty("-Dtest=value");
        assertEquals("test", pair.arg());
        assertEquals("value", pair.value());

        pair = SystemPropertyUtil.parseSystemProperty("-Dhmc.test=true");
        assertEquals("hmc.test", pair.arg());
        assertEquals("true", pair.value());

        pair = SystemPropertyUtil.parseSystemProperty("hmc.test=true");
        assertEquals("hmc.test", pair.arg());
        assertEquals("true", pair.value());

        pair = SystemPropertyUtil.parseSystemProperty("-Dhmc.option");
        assertEquals("hmc.option", pair.arg());
        assertNull(pair.value());
    }

}
