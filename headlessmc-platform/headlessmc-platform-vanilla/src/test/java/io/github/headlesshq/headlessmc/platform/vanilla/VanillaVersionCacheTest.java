package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.files.cache.*;
import io.github.headlesshq.headlessmc.platform.Vanilla;
import io.github.headlesshq.headlessmc.platform.util.PlatformCacheService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.FileSystems;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class VanillaVersionCacheTest {
    @Inject
    @Vanilla
    Cache<?> cache;

    @Inject
    JsonService jsonService;

    @Inject
    PlatformCacheService platformCacheService;

    @Test
    public void bundledResourceIsServedFirst() {
        // a fresh cache, so that no other test has already moved it to the local file or the http source
        Cache<VanillaManifest> bundled = platformCacheService.versionCache(
            new TypeLiteral<VanillaManifest>() {},
            Vanilla.PLATFORM_NAME,
            URI.create("https://invalid.invalid/vanilla-versions.json"),
            cache.getVersion()
        );

        VanillaManifest manifest = bundled.iterator().next();
        assertFalse(manifest.versions().isEmpty());
        assertNotNull(manifest.versions().getFirst().id());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getCache() {
        if (!Boolean.parseBoolean(System.getProperty("hmc.update.cachces", "false"))) {
            return;
        }

        CacheBuilder.create()
            .withVersion(cache.getVersion())
            .withStore(new JsonCacheFile<>(
                CacheExceptionHandler.throwing(),
                jsonService,
                new TypeLiteral<>() {},
                cache.getVersion(),
                FileSystems.getDefault().getPath("src/main/resources/vanilla/vanilla-versions.json")
            )).withSource((CacheSource<Object>) cache)
            .build()
            .get();
    }

}
