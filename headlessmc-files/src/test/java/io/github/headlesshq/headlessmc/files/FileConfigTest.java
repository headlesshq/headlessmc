package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileConfigTest {
    private record TestFileConfig(boolean local, boolean gameForEachVersion) implements FileConfig {
        @Override
        public Optional<String> location() {
            return Optional.empty();
        }

        @Override
        public Optional<String> mcDir() {
            return Optional.empty();
        }

        @Override
        public Optional<String> gameDir() {
            return Optional.empty();
        }
    }

    private final FileConfig config = new TestFileConfig(false, true);
    private final List<String> warnings = new ArrayList<>();

    @Test
    void returnsThePropertyWhenNoLegacyValueIsSet() {
        Optional<String> result =
            config.property(Optional.of("value"), Optional.empty(), warnings::add, "legacy", "current");

        assertEquals(Optional.of("value"), result);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void returnsEmptyWhenNeitherIsSet() {
        Optional<String> result =
            config.property(Optional.empty(), Optional.empty(), warnings::add, "legacy", "current");

        assertEquals(Optional.empty(), result);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void fallsBackToTheLegacyValueAndWarns() {
        Optional<String> result =
            config.property(Optional.empty(), Optional.of("old"), warnings::add, "legacy", "current");

        assertEquals(Optional.of("old"), result);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("legacy"));
        assertTrue(warnings.getFirst().contains("current"));
    }

    @Test
    void throwsWhenPropertyAndLegacyValuesAreSame() {
        assertThrows(
            ConfigException.class,
            () -> config.property(Optional.of("same"), Optional.of("same"), warnings::add, "legacy", "current")
        );
    }

    @Test
    void throwsWhenPropertyAndLegacyValuesConflict() {
        assertThrows(ConfigException.class,
            () -> config.property(Optional.of("new"), Optional.of("old"), warnings::add, "legacy", "current"));
    }

}
