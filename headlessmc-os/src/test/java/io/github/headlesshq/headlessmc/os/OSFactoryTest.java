package io.github.headlesshq.headlessmc.os;

import io.github.headlesshq.headlessmc.api.config.Config;
import io.github.headlesshq.headlessmc.api.config.ConfigImpl;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OSFactoryTest {
    @Test
    public void testOSFactory() {
        Properties properties = new Properties();
        properties.put(OsProperties.OS_ARCH.getName(), "true");
        properties.put(OsProperties.OS_NAME.getName(), "win");
        properties.put(OsProperties.OS_TYPE.getName(), "WINDOWS");
        properties.put(OsProperties.OS_ARCHITECTURE.getName(), "AMD64");
        properties.put(OsProperties.OS_VERSION.getName(), "10");
        Config config = new ConfigImpl(properties, "test", 0);

        OS os = OSFactory.detect(config);
        assertEquals("win", os.getName());
        assertEquals(OS.Type.WINDOWS, os.getType());
        assertEquals("10", os.getVersion());
        assertEquals("AMD64", os.getArchitecture());
        assertTrue(os.is64bit());
    }

    @Test
    public void testDarwinDetection() {
        Properties properties = new Properties();
        properties.put(OsProperties.OS_NAME.getName(), "darwin");
        Config config = new ConfigImpl(properties, "test", 0);

        OS os = OSFactory.detect(config);
        assertEquals(OS.Type.OSX, os.getType());
    }

}
