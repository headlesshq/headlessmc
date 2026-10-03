package io.github.headlesshq.headlessmc.launcher.assets;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.HttpVersion;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AssetsConfigTest {
    @Inject
    Holder<AssetsConfig> config;

    @Test
    public void testTopLevelDefaults() {
        AssetsConfig assets = config.get();
        assertFalse(assets.dummy());
        assertEquals("https://resources.download.minecraft.net", assets.url().toString());
    }

    @Test
    public void testNestedNetConfig() {
        // hmc.assets.net.* is a nested NetConfig with overrides from application.properties.
        AssetsConfig assets = config.get();
        assertEquals(0, assets.net().retries());
        assertTrue(assets.net().cookies());
        assertTrue(assets.net().httpVersion().isEmpty());
    }

    @Test
    public void testNestedParallelConfigUsesDefaults() {
        // hmc.assets.parallel.* is a nested ParallelConfig with its own defaults.
        assertEquals(6, config.get().parallel().parallelism());
    }

    @Test
    public void testHttp1WrapperForcesHttpVersionWhenUnset() {
        AssetsConfig wrapped = new Http1AssetsConfig(config.get());
        assertTrue(wrapped.net().httpVersion().isPresent());
        assertEquals(HttpVersion.HTTP_1_1, wrapped.net().httpVersion().get());
        // other values still pass through
        assertEquals(0, wrapped.net().retries());
        assertTrue(wrapped.net().cookies());
    }

}
