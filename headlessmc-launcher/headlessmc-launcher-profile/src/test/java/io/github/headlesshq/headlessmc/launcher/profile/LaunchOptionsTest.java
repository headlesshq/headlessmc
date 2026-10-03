package io.github.headlesshq.headlessmc.launcher.profile;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class LaunchOptionsTest {
    private static final LaunchOptions EMPTY =
        new LaunchOptions(Optional.empty(), Optional.empty(), Optional.empty(), false);

    @Test
    public void parsesResolution() {
        assertEquals(new LaunchOptions.Resolution(800, 600), LaunchOptions.Resolution.parse("800x600"));
        assertEquals(new LaunchOptions.Resolution(1920, 1080), LaunchOptions.Resolution.parse("1920 X 1080"));
        assertEquals("800x600", new LaunchOptions.Resolution(800, 600).toString());
    }

    @Test
    public void invalidResolutionThrows() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.Resolution.parse("800"));
        assertThrows(NumberFormatException.class, () -> LaunchOptions.Resolution.parse("axb"));
    }

    @Test
    public void emptyOptions() {
        assertTrue(EMPTY.isEmpty());
        assertFalse(EMPTY.withDemo(true).isEmpty());
        assertFalse(EMPTY.withResolution(Optional.of(new LaunchOptions.Resolution(1, 1))).isEmpty());
    }

    @Test
    public void mergePrefersOwnValues() {
        LaunchOptions defaults = new LaunchOptions(
            Optional.of(new LaunchOptions.Resolution(800, 600)),
            Optional.of("default-path"),
            Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, "default.com")),
            true
        );
        LaunchOptions own = new LaunchOptions(
            Optional.of(new LaunchOptions.Resolution(1024, 768)),
            Optional.empty(),
            Optional.empty(),
            false
        );

        LaunchOptions merged = own.mergeWithDefaults(defaults);

        assertEquals(new LaunchOptions.Resolution(1024, 768), merged.resolution().orElseThrow());
        assertEquals("default-path", merged.quickPlayPath().orElseThrow());
        assertEquals("default.com", merged.join().orElseThrow().target());
        assertFalse(merged.demo(), "demo is not inherited from defaults");
    }

}
