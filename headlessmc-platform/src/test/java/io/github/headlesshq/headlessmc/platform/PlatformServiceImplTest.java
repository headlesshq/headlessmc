package io.github.headlesshq.headlessmc.platform;

import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest
class PlatformServiceImplTest {
    /** Makes the fake platforms available as beans, just like the real platform modules do. */
    static class Platforms {
        @Produces
        @Singleton
        VanillaPlatform vanilla() {
            return new FakeVanillaPlatform("1.21.1");
        }

        @Produces
        @Singleton
        Platform fabric() {
            return FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        }
    }

    @Inject
    PlatformServiceImpl service;

    @Test
    void listsAllPlatforms() {
        assertEquals(
            List.of("fabric", "vanilla"),
            service.getPlatforms().stream().map(Platform::getName).sorted(Comparator.naturalOrder()).toList()
        );
    }

    @Test
    void listsPlatformsWithoutVanilla() {
        assertEquals(List.of("fabric"), service.getPlatformsWithoutVanilla().stream().map(Platform::getName).toList());
    }

    @Test
    void resolvesPlatformsByNameIgnoringCase() {
        assertEquals(Optional.of("fabric"), service.getPlatform("FABRIC").map(Platform::getName));
        assertEquals(Optional.empty(), service.getPlatform("forge"));
    }

    @Test
    void exposesTheVanillaPlatform() {
        assertSame(service.getVanillaPlatform(), service.getPlatform("vanilla").orElseThrow());
    }

}
