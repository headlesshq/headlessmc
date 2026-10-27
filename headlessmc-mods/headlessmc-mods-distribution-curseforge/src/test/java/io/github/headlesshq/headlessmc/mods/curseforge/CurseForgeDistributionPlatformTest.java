package io.github.headlesshq.headlessmc.mods.curseforge;

import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the platform wires up in a real container.
 * What it does once wired is covered by {@link CurseForgePlatformTest}.
 * The tests against the real CurseForge API only run if an api key
 * has been provided via the {@code HMC_MODS_CURSEFORGE_API_KEY} environment variable.
 */
@QuarkusTest
public class CurseForgeDistributionPlatformTest {
    private static final String API_KEY_ENV = "HMC_MODS_CURSEFORGE_API_KEY";

    @Inject
    CurseForgeDistributionPlatform platform;

    @Inject
    @RestClient
    CurseForgeAPI api;

    @TempDir
    Path dir;

    @Test
    public void testCurseForgeDistributionPlatform() {
        assertNotNull(platform);
        assertEquals(CurseForgeDistributionPlatform.NAME, platform.getName());
    }

    @ParameterizedTest
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    @ValueSource(strings = {"hmc-specifics", "hmc-optimizations", "hmc-prometheus", "hmc-mcp-bridge", "mc-runtime-check"})
    public void testFindsAndDownloadsHeadlessHqMods(String slug) throws Exception {
        assertTrue(platform.search(slug, ModType.MOD).stream().anyMatch(mod -> slug.equals(mod.id())),
            "search did not find " + slug);

        String apiKey = System.getenv(API_KEY_ENV);
        CurseForgeMod mod = api.search(apiKey, CurseForgeIds.MINECRAFT, 6, null, slug, null, null, null, null)
            .data().getFirst();
        CurseForgeFile newest = api.getFiles(apiKey, mod.id(), null, null).data().stream()
            .max(Comparator.comparing(CurseForgeFile::fileDate))
            .orElseThrow();

        String loader = newest.gameVersions().stream()
            .map(v -> v.toLowerCase(Locale.ENGLISH))
            .filter(v -> CurseForgeIds.modLoaderType(v) != null)
            .findFirst()
            .orElseThrow();
        String version = newest.gameVersions().stream()
            .filter(v -> Character.isDigit(v.charAt(0)))
            .findFirst()
            .orElseThrow();

        Path file = platform.download(dir, null, slug, VersionArg.parse(List.of(loader, version)), ModType.MOD);

        assertTrue(Files.size(file) > 0);
        System.out.printf("Downloaded %s (%s %s) to %s, %d bytes%n", slug, loader, version, file, Files.size(file));
    }

}
