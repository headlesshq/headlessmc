package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.files.cache.*;
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
public class ForgeVersionCacheTest {
    @Inject
    @Forge
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
            Forge.PLATFORM_NAME,
            URI.create("https://invalid.invalid/forge-versions.json"),
            cache.getVersion()
        );

        PrismIndex index = bundled.iterator().next();
        assertEquals("net.minecraftforge", index.uid());
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
                FileSystems.getDefault().getPath("src/main/resources/forge/forge-versions.json")
            )).withSource((CacheSource<Object>) cache)
            .build()
            .get();
    }

}
