package io.github.headlesshq.headlessmc.platform.util;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.View;
import io.github.headlesshq.headlessmc.platform.AbstractCachedVersionService;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VersionService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.*;

@Slf4j
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractJsonVersionService<V extends PlatformVersion, J>
    extends AbstractCachedVersionService<V> implements VersionService {

    private final Cache<J> cache;
    private final View<SequencedSet<V>> setView;
    private final View<Map<String, V>> mapView;
    private final View<Map<V, Integer>> indices;

    public AbstractJsonVersionService(String platformName, Cache<J> cache) {
        super(platformName);
        this.cache = cache;
        this.setView = cache.view(this::map);
        this.mapView = createMapView(setView);
        this.indices = createIndices(setView);
    }

    // it would be nice if we could hide type parameter J as an implementation detail
    protected abstract SequencedSet<V> map(J value, @Nullable SequencedSet<V> previous);

    // TODO: this is currently broken:
    // [io.github.headlesshq.headlessmc.platform.util.AbstractJsonVersionService] (main) Platform version changed at index 800, expected 1.6.3 but got 13w37b in VanillaManifest[versions=[Version[id=26.3-snapshot-9, type=snapshot, url=https://piston-meta.mojang.com/v1/packages/cc88c84a24aa6e73ec4b0c7b2237a36056b23563/26.3-snapshot-9.json], Versi
    protected SequencedSet<V> verify(J value, SequencedSet<V> result, @Nullable SequencedSet<V> previous) {
        if (previous != null) {
            if (!result.containsAll(previous)) {
                Set<V> missing = new HashSet<>(previous);
                missing.removeAll(result);
                log.error("Missing versions after update: {}", missing);
            }
        }

        return result;
    }

}
