package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.files.cache.*;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;

@QuarkusTest
public class NeoForgeVersionCacheTest {
    @Inject
    @NeoForge
    Cache<?> cache;

    @Inject
    JsonService jsonService;

    @Test
    @SuppressWarnings("unchecked")
    public void getCache() {
        if (!Boolean.parseBoolean(System.getProperty("hmc.update.cachces", "false"))) {
            return;
        }

        CacheBuilder.create()
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
