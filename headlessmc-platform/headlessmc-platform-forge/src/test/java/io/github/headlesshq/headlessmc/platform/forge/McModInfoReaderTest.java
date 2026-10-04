package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.wildfly.common.Assert.assertTrue;

@QuarkusTest
public class McModInfoReaderTest {
    @Inject
    @Forge
    McModInfoReader mcModInfoReader;

    @Test
    public void testMcModInfoReader() throws IOException, HeadlessMcException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mcmod.info")) {
            assert inputStream != null;
            Optional<List<Mod>> result = mcModInfoReader.readEntry(inputStream);
            assertTrue(result.isPresent());
            List<Mod> mods = result.get();
            List<Mod> expected = List.of(
                new Mod(
                    "examplemod",
                    "Example Mod",
                    Optional.of("Example placeholder mod."),
                    List.of("ExampleDude"),
                    Map.of()
                ),
                new Mod(
                    "examplemod2",
                    "Example Mod",
                    Optional.of("Example placeholder mod.2"),
                    List.of("ExampleDude2"),
                    Map.of()
                )
            );

            assertEquals(expected, mods);
        }
    }

}
