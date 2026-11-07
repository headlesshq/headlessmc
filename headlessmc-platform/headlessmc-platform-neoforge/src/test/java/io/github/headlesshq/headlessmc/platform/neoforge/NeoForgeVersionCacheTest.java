package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.files.cache.*;
import io.github.headlesshq.headlessmc.platform.forge.PrismIndex;
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
public class NeoForgeVersionCacheTest {
    @Inject
    @NeoForge
    Cache<?> cache;

    @Inject
    JsonService jsonService;

    @Inject
    PlatformCacheService platformCacheService;

    @Test
    public void bundledResourceIsServedFirst() {
        // a fresh cache, so that no other test has already moved it to the local file or the http source
        Cache<PrismIndex> bundled = platformCacheService.versionCache(
            new TypeLiteral<PrismIndex>() {},
            NeoForge.PLATFORM_NAME,
            URI.create("https://invalid.invalid/neoforge-versions.json"),
            cache.getVersion()
        );

        PrismIndex index = bundled.iterator().next();
        assertEquals("net.neoforged", index.uid());
        assertFalse(index.versions().isEmpty());
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
                FileSystems.getDefault().getPath("src/main/resources/neoforge/neoforge-versions.json")
            )).withSource((CacheSource<Object>) cache)
            .build()
            .get();
    }

}
