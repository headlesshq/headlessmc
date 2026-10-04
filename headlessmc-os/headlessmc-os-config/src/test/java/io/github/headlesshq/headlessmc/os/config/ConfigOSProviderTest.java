package io.github.headlesshq.headlessmc.os.config;

import io.github.headlesshq.headlessmc.os.OS;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class ConfigOSProviderTest {
    @Inject
    OS os;

    @Test
    public void testConfigOSProvider() {
        // configured from src/test/resources/application.properties
        assertEquals("test-os", os.name());
        assertEquals("1.0.0", os.version());
        assertEquals("TestOS", os.type().mcType().name());
    }

}
