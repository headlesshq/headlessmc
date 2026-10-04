package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.files.cache.View;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.*;

class AbstractCachedVersionServiceTest {
    /** A cached version service over a list of "vanillaVersion:build" strings. */
    private static final class TestService extends AbstractCachedVersionService<BoundPlatformVersion> {
        private final Cache<List<String>> cache;
        private final View<SequencedSet<BoundPlatformVersion>> setView;
        private final View<Map<String, BoundPlatformVersion>> mapView;
        private final View<Map<BoundPlatformVersion, Integer>> indices;

        TestService(Cache<List<String>> cache) {
            super("fabric");
            this.cache = cache;
            this.setView = cache.view((value, previous) -> {
                SequencedSet<BoundPlatformVersion> result = new LinkedHashSet<>();
                for (String entry : value) {
                    String[] parts = entry.split(":");
                    result.add(createVersion(parts[1], parts[0]));
                }

                return result;
            });
            this.mapView = createMapView(setView);
            this.indices = createIndices(setView);
        }

        @Override
        protected View<SequencedSet<BoundPlatformVersion>> getSetView() {
            return setView;
        }

        @Override
        protected View<Map<String, BoundPlatformVersion>> getMapView() {
            return mapView;
        }

        @Override
        protected View<Map<BoundPlatformVersion, Integer>> getIndices() {
            return indices;
        }

        @Override
        public SequencedSet<BoundPlatformVersion> getBuilds(VanillaVersion version) {
            return filter(getVersions(), version);
        }

        Cache<List<String>> cache() {
            return cache;
        }
    }

    private final List<String> entries = new ArrayList<>(
        List.of("1.21.1:0.16.9", "1.21.1:0.16.5", "1.20.4:0.15.0")
    );

    private TestService service;

    @BeforeEach
    void setup() {
        Cache<List<String>> cache = CacheBuilder.<List<String>>create()
            .withSource(() -> Optional.of(entries))
            .build();
        service = new TestService(cache);
    }

    private VanillaVersion vanilla(String name) {
        return new FakeVanillaPlatform(name).getVersionService().getVersion(name).orElseThrow();
    }

    @Test
    void platformNameIsExposed() {
        assertEquals("fabric", service.getPlatformName());
    }

    @Test
    void listsAllVersionsInCacheOrder() {
        assertEquals(
            List.of("0.16.9", "0.16.5", "0.15.0"),
            service.getVersions().stream().map(PlatformVersion::getName).toList()
        );
    }

    @Test
    void emptyCacheThrows() {
        TestService empty = new TestService(
            CacheBuilder.<List<String>>create().withSource(Optional::empty).build()
        );

        assertThrows(NotFoundException.class, empty::getVersions);
    }

    @Test
    void filtersBuildsByVanillaVersion() {
        assertEquals(
            List.of("0.16.9", "0.16.5"),
            service.getBuilds(vanilla("1.21.1")).stream().map(PlatformVersion::getName).toList()
        );
    }

    @Test
    void resolvesLatestBuild() {
        assertEquals("0.16.9", service.getLatestBuild(vanilla("1.21.1")).orElseThrow().getName());
        assertEquals(Optional.empty(), service.getLatestBuild(vanilla("1.7.10")));
    }

    @Test
    void resolvesSpecificBuild() {
        assertEquals("0.16.5", service.getBuild(vanilla("1.21.1"), "0.16.5").orElseThrow().getName());
        assertEquals(Optional.empty(), service.getBuild(vanilla("1.21.1"), "9.9.9"));
    }

    @Test
    void mapViewIsKeyedByBuildName() {
        Map<String, BoundPlatformVersion> map = service.getMapView().get().orElseThrow();

        assertEquals("0.16.9", map.get("0.16.9").getName());
        assertEquals(3, map.size());
    }

    @Test
    void sortsVersionsByTheirCacheIndex() {
        List<PlatformVersion> shuffled = new ArrayList<>(List.of(
            service.getBuild(vanilla("1.20.4"), "0.15.0").orElseThrow(),
            service.getBuild(vanilla("1.21.1"), "0.16.9").orElseThrow()
        ));

        assertEquals(
            List.of("0.16.9", "0.15.0"),
            service.sort(shuffled).stream().map(PlatformVersion::getName).toList()
        );
    }

    @Test
    void sortingUnknownVersionsThrows() {
        List<PlatformVersion> unknown = new ArrayList<>(
            List.of(new FakePlatform("fabric", service).version("1.21.1", "9.9.9"))
        );

        NotFoundException e = assertThrows(NotFoundException.class, () -> service.sort(unknown));
        assertTrue(e.getMessage().contains("Failed to find versions"));
    }

    @Test
    void viewsUpdateWhenTheCacheChanges() {
        service.cache().set(new ArrayList<>(List.of("1.21.1:0.17.0")));

        assertEquals(List.of("0.17.0"), service.getVersions().stream().map(PlatformVersion::getName).toList());
    }

    @Test
    void createVanillaVersionIsRejectedForOtherPlatforms() {
        assertThrows(IllegalArgumentException.class, () -> service.createVanillaVersion("1.21.1"));
    }

    @Test
    void createsUnboundPlatformVersions() {
        PlatformVersion version = service.createVersion("0.16.9");

        assertEquals("fabric", version.getPlatformName());
        assertEquals("fabric/0.16.9", version.toString());
    }

}
