package io.github.headlesshq.headlessmc.launcher.client;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SpecificsPropertiesServiceTest {
    private static Config config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
            .addDefaultInterceptors()
            .withSources(new PropertiesConfigSource(properties, "test", 100))
            .build();
    }

    private static Map<String, String> defaults() {
        Map<String, String> defaults = new HashMap<>();
        for (SpecificsProperty property : SpecificsProperty.ALL) {
            if (property.defaultValue() != null) {
                defaults.put(property.name(), property.defaultValue().toString());
            }
        }

        return defaults;
    }

    @Test
    public void testDefaultsAreNotPassed() {
        assertTrue(SpecificsPropertiesService.getSystemProperties(config(Map.of())).isEmpty());
        assertTrue(SpecificsPropertiesService.getSystemProperties(config(defaults())).isEmpty());
    }

    @Test
    public void testDeviatingValuesArePassed() {
        Map<String, String> properties = defaults();
        properties.put("hmc.jline.enabled", "false");
        properties.put("hmc.account.refresh.interval", "60");
        properties.put("hmc.files.location", "/some/dir");

        assertEquals(
            Map.of("hmc.jline.enabled", "false", "hmc.account.refresh.interval", "60", "hmc.files.location", "/some/dir"),
            SpecificsPropertiesService.getSystemProperties(config(properties))
        );
    }

    @Test
    public void testValuesAreConverted() {
        Map<String, String> properties = defaults();
        properties.put("hmc.jline.enabled", "off");
        properties.put("hmc.jline.dumb", "on");
        properties.put("hmc.account.refresh.enabled", "TRUE");

        assertEquals(
            Map.of("hmc.jline.enabled", "false", "hmc.jline.dumb", "true"),
            SpecificsPropertiesService.getSystemProperties(config(properties))
        );
    }

    @Test
    public void testInvalidValuesAreSkipped() {
        Map<String, String> properties = defaults();
        properties.put("hmc.account.refresh.interval", "not a number");
        assertTrue(SpecificsPropertiesService.getSystemProperties(config(properties)).isEmpty());
    }

}
