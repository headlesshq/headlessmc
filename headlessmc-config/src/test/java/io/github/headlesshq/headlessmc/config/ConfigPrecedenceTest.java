package io.github.headlesshq.headlessmc.config;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests how the {@link ConfigServiceImpl} orders its own properties
 * relative to system properties and application.properties.
 */
@QuarkusTest
public class ConfigPrecedenceTest {
    private static final String PROP = "hmc.test.properties.test-property";
    private static final String INT = "hmc.test.properties.test-int";

    @Inject
    @Any
    Instance<DynamicConfig> dynamicConfigs;

    @AfterEach
    public void clearSystemProperties() {
        System.clearProperty(PROP);
        System.clearProperty(INT);
    }

    private ConfigServiceImpl newConfigService() {
        SmallRyeConfig base = new SmallRyeConfigBuilder()
            .withSources(new PropertiesConfigSource(
                Map.of(PROP, "fromApplicationProperties", INT, "1"), "application.properties", 250
            ))
            .withSources(new SysPropConfigSource())
            .build();
        return new ConfigServiceImpl(dynamicConfigs, new ConcurrentHashMap<>(), base);
    }

    @Test
    public void applicationPropertiesAreUsedWithoutOverrides() {
        ConfigServiceImpl configService = newConfigService();
        assertEquals("fromApplicationProperties", configService.getConfig().getValue(PROP, String.class));
        assertEquals(
            "fromApplicationProperties",
            configService.getHolder(TestConfig.class).get().testProperty()
        );
    }

    @Test
    public void systemPropertiesTakePrecedenceOverApplicationProperties() {
        System.setProperty(PROP, "fromSystem");
        ConfigServiceImpl configService = newConfigService();
        assertEquals("fromSystem", configService.getConfig().getValue(PROP, String.class));
        assertEquals("fromSystem", configService.getHolder(TestConfig.class).get().testProperty());
    }

    @Test
    public void setAtRuntimeTakesPrecedenceOverSystemProperties() {
        System.setProperty(PROP, "fromSystem");
        ConfigServiceImpl configService = newConfigService();
        assertTrue(configService.set(PROP, "fromSet", false));
        assertEquals("fromSet", configService.getConfig().getValue(PROP, String.class));
        assertEquals("fromSet", configService.getHolder(TestConfig.class).get().testProperty());
    }

    @Test
    public void removeFallsBackToSystemProperties() {
        System.setProperty(PROP, "fromSystem");
        ConfigServiceImpl configService = newConfigService();
        configService.set(PROP, "fromSet", false);
        assertTrue(configService.remove(PROP));
        assertEquals("fromSystem", configService.getConfig().getValue(PROP, String.class));
    }

    @Test
    public void setReturnsTrueEvenIfASystemPropertyAlreadyHadThatValue() {
        System.setProperty(PROP, "same");
        ConfigServiceImpl configService = newConfigService();
        assertTrue(configService.set(PROP, "same", false));
    }

    @Test
    public void invalidSystemPropertyFailsTheConstruction() {
        System.setProperty(INT, "notAnInt");
        assertThrows(Exception.class, this::newConfigService);
    }

}
