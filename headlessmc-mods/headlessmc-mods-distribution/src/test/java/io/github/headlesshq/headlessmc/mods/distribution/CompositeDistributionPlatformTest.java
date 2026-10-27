package io.github.headlesshq.headlessmc.mods.distribution;

import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompositeDistributionPlatformTest {
    private static final Path DIR = Path.of("mods");
    private static final VersionArg VERSION = VersionArg.parse("fabric", "1.21.1");

    private final List<String> downloadAttempts = new ArrayList<>();

    @Test
    void searchCombinesTheResultsOfEnabledPlatforms() {
        CompositeDistributionPlatform platform = new CompositeDistributionPlatform("all", List.of(
            new TestPlatform("a", true, false, false), new TestPlatform("b", false, false, false), new TestPlatform("c", true, false, false)
        ));

        assertEquals(
            Map.of("a", List.of(mod("a")), "c", List.of(mod("c"))),
            platform.searchByPlatform("query", VERSION, ModType.MOD)
        );
        assertEquals(List.of(mod("a"), mod("c")), platform.search("query", ModType.MOD));
    }

    @Test
    void searchSkipsFailingPlatforms() {
        CompositeDistributionPlatform platform = new CompositeDistributionPlatform("all", List.of(
            new TestPlatform("a", true, true, false), new TestPlatform("b", true, false, false)
        ));

        assertEquals(List.of(mod("b")), platform.search("query", VERSION, ModType.MOD));
    }

    @Test
    void searchThrowsIfAllPlatformsFail() {
        CompositeDistributionPlatform platform = new CompositeDistributionPlatform("all", List.of(
            new TestPlatform("a", true, true, false), new TestPlatform("b", true, true, false)
        ));

        ModDistributionException e = assertThrows(
            ModDistributionException.class, () -> platform.search("query", ModType.MOD)
        );
        assertEquals(2, e.getSuppressed().length);
    }

    @Test
    void downloadReturnsTheFirstSuccessfulDownload() {
        CompositeDistributionPlatform platform = new CompositeDistributionPlatform("default", List.of(
            new TestPlatform("a", true, false, true),
            new TestPlatform("b", false, false, false),
            new TestPlatform("c", true, false, false),
            new TestPlatform("d", true, false, false)
        ));

        assertEquals(DIR.resolve("c.jar"), platform.download(DIR, null, "id", VERSION, ModType.MOD));
        assertEquals(List.of("a", "c"), downloadAttempts);
    }

    @Test
    void downloadThrowsIfAllPlatformsFail() {
        CompositeDistributionPlatform platform = new CompositeDistributionPlatform("default", List.of(
            new TestPlatform("a", true, false, true), new TestPlatform("b", true, false, true)
        ));

        ModDistributionException e = assertThrows(
            ModDistributionException.class, () -> platform.download(DIR, null, "id", VERSION, ModType.MOD)
        );
        assertTrue(e.getMessage().contains("a failed"));
        assertTrue(e.getMessage().contains("b failed"));
        assertEquals(2, e.getSuppressed().length);
    }

    @Test
    void isOnlyEnabledIfAPlatformIsEnabled() {
        CompositeDistributionPlatform disabled = new CompositeDistributionPlatform("all", List.of(
            new TestPlatform("a", false, false, false)
        ));

        assertFalse(disabled.isEnabled());
        assertTrue(disabled.getPlatforms().isEmpty());
        assertThrows(ModDistributionException.class, () -> disabled.search("query", ModType.MOD));
    }

    private static RemoteMod mod(String platform) {
        return new RemoteMod(platform + "-mod", platform, platform);
    }

    private class TestPlatform implements ModDistributionPlatform {
        private final String name;
        private final boolean enabled;
        private final boolean searchFails;
        private final boolean downloadFails;

        TestPlatform(String name, boolean enabled, boolean searchFails, boolean downloadFails) {
            this.name = name;
            this.enabled = enabled;
            this.searchFails = searchFails;
            this.downloadFails = downloadFails;
        }

        @Override
        public List<RemoteMod> search(String query, ModType type) throws ModDistributionException {
            if (searchFails) {
                throw new ModDistributionException(name + " failed");
            }

            return List.of(mod(name));
        }

        @Override
        public List<RemoteMod> search(String query, VersionArg version, ModType type) throws ModDistributionException {
            return search(query, type);
        }

        @Override
        public Path download(Path dir, @Nullable Path worldDir, String id, VersionArg version, ModType type)
            throws ModDistributionException {
            downloadAttempts.add(name);
            if (downloadFails) {
                throw new ModDistributionException(name + " failed");
            }

            return dir.resolve(name + ".jar");
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String getName() {
            return name;
        }
    }

}
