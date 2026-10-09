package io.github.headlesshq.headlessmc.patcher;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PatchResultTest {
    private static PatchResult empty() {
        return new PatchResult(new LinkedHashSet<>(), new LinkedHashSet<>());
    }

    @Test
    public void newPatchResultHasNoSystemProperties() {
        assertEquals(Map.of(), empty().systemProperties());
    }

    @Test
    public void withSystemPropertyAddsPropertyWithoutModifyingTheOriginal() {
        PatchResult original = empty();

        PatchResult result = original.withSystemProperty("joml.nounsafe", "true");

        assertEquals(Map.of("joml.nounsafe", "true"), result.systemProperties());
        assertEquals(Map.of(), original.systemProperties());
    }

    @Test
    public void withSystemPropertyReplacesExistingValue() {
        PatchResult result = empty()
            .withSystemProperty("key", "first")
            .withSystemProperty("key", "second");

        assertEquals(Map.of("key", "second"), result.systemProperties());
    }

    @Test
    public void systemPropertiesCanHaveNoValue() {
        PatchResult result = empty().withSystemProperty("flag", null);

        assertTrue(result.systemProperties().containsKey("flag"));
        assertNull(result.systemProperties().get("flag"));
    }

    @Test
    public void systemPropertiesAreUnmodifiable() {
        PatchResult result = empty().withSystemProperty("key", "value");

        assertThrows(UnsupportedOperationException.class, () -> result.systemProperties().put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> empty().systemProperties().put("other", "value"));
    }

    @Test
    public void addingFilesAndAgentsKeepsSystemProperties() {
        Path file = Path.of("lib.jar");
        Path agent = Path.of("agent.jar");

        PatchResult result = empty()
            .withSystemProperty("key", "value")
            .withFile(file)
            .withAgent(agent);

        assertEquals(Map.of("key", "value"), result.systemProperties());
        assertEquals(new LinkedHashSet<>(List.of(file)), result.files());
        assertEquals(new LinkedHashSet<>(List.of(agent)), result.javaAgents());
    }

}
