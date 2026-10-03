package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.Vanilla;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.util.AbstractJsonVersionService;
import io.quarkus.runtime.annotations.RegisterResources;
import org.jspecify.annotations.Nullable;

import java.util.*;

@RegisterResources(globs = "vanilla/vanilla-versions.json")
class VanillaVersionServiceImpl extends AbstractJsonVersionService<VanillaVersion, VanillaManifest>
    implements VanillaVersionService, VersionService {

    public VanillaVersionServiceImpl(Cache<VanillaManifest> cache) {
        super(Vanilla.PLATFORM_NAME, cache);
    }

    @Override
    protected SequencedSet<VanillaVersion> map(VanillaManifest value, @Nullable SequencedSet<VanillaVersion> previous) {
        SequencedSet<VanillaVersion> result = new LinkedHashSet<>();
        for (VanillaManifest.Version version : value.versions()) {
            result.add(createVanillaVersion(version.id()));
        }

        return verify(value, result, previous);
    }

    @Override
    public VanillaVersion getLatest() throws HeadlessMcException {
        return getVersions().getFirst();
    }

    @Override
    public boolean hasVersion(String name) throws HeadlessMcException {
        return getMapView().stream().anyMatch(map -> map.containsKey(name));
    }

    @Override
    public Optional<VanillaVersion> getVersion(String name) throws HeadlessMcException {
        return getMapView().stream()
            .map(map -> map.get(name))
            .filter(Objects::nonNull)
            .findFirst();
    }

    @Override
    public SequencedSet<VanillaVersion> getBuilds(VanillaVersion version) {
        return new LinkedHashSet<>(List.of(version));
    }

    @Override // expose for use in package
    protected Cache<VanillaManifest> getCache() {
        return super.getCache();
    }

}
