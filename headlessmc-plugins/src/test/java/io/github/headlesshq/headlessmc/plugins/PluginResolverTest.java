package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.plugins.version.Version;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PluginResolverTest {
    private final PluginResolver resolver = new PluginResolver(
        new CoreLibraries(Map.of("org.apache.commons:commons-lang3", Version.parse("3.21.0"))),
        "3.0.0-RC8"
    );

    @Test
    void testHighestCompatibleLibraryVersionWins() {
        PluginFile a = plugin("a", lib("com.example", "lib", "1.2.0", null));
        PluginFile b = plugin("b", lib("com.example", "lib", "1.5.0", null));
        PluginResolution resolution = resolver.resolve(List.of(a, b));
        assertEquals(List.of(), resolution.problems());
        assertEquals(List.of(a, b), resolution.plugins());
        assertEquals(1, resolution.libraries().size());
        assertEquals("1.5.0", resolution.libraries().getFirst().library().version());
        assertEquals(b, resolution.libraries().getFirst().source());
        assertEquals(List.of("a", "b"), resolution.libraries().getFirst().requestedBy());
    }

    @Test
    void testDifferentMajorVersionsConflict() {
        PluginFile a = plugin("a", lib("com.example", "lib", "1.2.0", null));
        PluginFile b = plugin("b", lib("com.example", "lib", "2.0.0", null));
        PluginFile c = plugin("c");
        PluginResolution resolution = resolver.resolve(List.of(a, b, c));
        assertEquals(List.of(c), resolution.plugins());
        assertEquals(2, resolution.problems().size());
        assertTrue(resolution.problemsOf(a).getFirst().message().contains("conflicting versions of library com.example:lib"));
    }

    @Test
    void testExplicitRangeAllowsOlderVersion() {
        // b requires at least 1.4, a only ships 1.2, so the 1.5 of b is used
        PluginFile a = plugin("a", lib("com.example", "lib", "1.2.0", "[1.0,3)"));
        PluginFile b = plugin("b", lib("com.example", "lib", "1.5.0", "[1.4,2)"));
        PluginResolution resolution = resolver.resolve(List.of(a, b));
        assertEquals(List.of(), resolution.problems());
        assertEquals("1.5.0", resolution.libraries().getFirst().library().version());
    }

    @Test
    void testNoShippedVersionInIntersection() {
        PluginFile a = plugin("a", lib("com.example", "lib", "1.2.0", "[1.0,1.3)"));
        PluginFile b = plugin("b", lib("com.example", "lib", "1.5.0", "[1.4,2)"));
        PluginResolution resolution = resolver.resolve(List.of(a, b));
        assertEquals(List.of(), resolution.plugins());
    }

    @Test
    void testCoreLibrariesArePinned() {
        PluginFile compatible = plugin("compatible", lib("org.apache.commons", "commons-lang3", "3.12.0", null));
        PluginFile tooNew = plugin("too-new", lib("org.apache.commons", "commons-lang3", "3.25.0", null));
        PluginResolution resolution = resolver.resolve(List.of(compatible, tooNew));
        assertEquals(List.of(compatible), resolution.plugins());
        assertEquals(List.of(), resolution.libraries());
        assertTrue(resolution.problemsOf(tooNew).getFirst().message().contains("HeadlessMc ships 3.21.0"));
    }

    @Test
    void testHeadlessMcVersion() {
        PluginFile compatible = plugin("compatible");
        PluginFile incompatible = new PluginFile(Path.of("incompatible.jar"), "incompatible",
            manifest("incompatible", "1.0.0", "[3.0.0,4)", List.of(), List.of()), null);
        PluginResolution resolution = resolver.resolve(List.of(compatible, incompatible));
        assertEquals(List.of(compatible), resolution.plugins());
        assertTrue(resolution.problemsOf(incompatible).getFirst().message().contains("requires HeadlessMc"));
    }

    @Test
    void testDependenciesAreRejectedTransitively() {
        PluginFile base = new PluginFile(Path.of("base.jar"), "base",
            manifest("base", "1.0.0", "3.0.0-RC8", List.of(), List.of(lib("com.example", "lib", "1.0", null))), null);
        PluginFile other = new PluginFile(Path.of("other.jar"), "other",
            manifest("other", "1.0.0", "3.0.0-RC8", List.of(), List.of(lib("com.example", "lib", "2.0", null))), null);
        PluginFile addon = new PluginFile(Path.of("addon.jar"), "addon", manifest("addon", "1.0.0", "3.0.0-RC8",
            List.of(new PluginManifest.Dependency("base", "[1.0,2)", false)), List.of()), null);
        PluginFile optional = new PluginFile(Path.of("optional.jar"), "optional", manifest("optional", "1.0.0", "3.0.0-RC8",
            List.of(new PluginManifest.Dependency("missing", "*", true)), List.of()), null);
        PluginResolution resolution = resolver.resolve(List.of(base, other, addon, optional));
        // base and other conflict, so addon cannot be loaded either
        assertEquals(List.of(optional), resolution.plugins());
        assertTrue(resolution.problemsOf(addon).getFirst().message().contains("requires plugin 'base'"));
    }

    @Test
    void testDependencyVersion() {
        PluginFile base = plugin("base");
        PluginFile addon = new PluginFile(Path.of("addon.jar"), "addon", manifest("addon", "1.0.0", "3.0.0-RC8",
            List.of(new PluginManifest.Dependency("base", "[2.0,3)", false)), List.of()), null);
        PluginResolution resolution = resolver.resolve(List.of(base, addon));
        assertEquals(List.of(base), resolution.plugins());
    }

    @Test
    void testDuplicateIdsAndInvalidFiles() {
        PluginFile first = plugin("same");
        PluginFile second = new PluginFile(Path.of("same-2.jar"), "same2", first.manifest(), null);
        PluginFile invalid = new PluginFile(Path.of("invalid.jar"), "invalid", null, "not a HeadlessMc plugin");
        PluginResolution resolution = resolver.resolve(List.of(first, second, invalid));
        assertEquals(List.of(), resolution.plugins());
        assertEquals(3, resolution.problems().size());
    }

    private static PluginFile plugin(String id, PluginManifest.Library... libraries) {
        return new PluginFile(Path.of(id + ".jar"), id, manifest(id, "1.0.0", "3.0.0-RC8", List.of(), List.of(libraries)), null);
    }

    static PluginManifest manifest(
        String id,
        String version,
        String headlessmc,
        List<PluginManifest.Dependency> dependencies,
        List<PluginManifest.Library> libraries
    ) {
        return new PluginManifest(1, id, version, null, null, null, null, null, headlessmc, null,
            new ArrayList<>(dependencies), new ArrayList<>(libraries));
    }

    private static PluginManifest.Library lib(String group, String artifact, String version, @Nullable String versions) {
        return new PluginManifest.Library(group, artifact, version, versions,
            "META-INF/jars/" + artifact + "-" + version + ".jar", "0");
    }

}
